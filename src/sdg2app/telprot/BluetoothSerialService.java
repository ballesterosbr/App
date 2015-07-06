package sdg2app.telprot;

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothSerialService {

	// Debugging
	private static final String TAG = "BluetoothAppService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME = "BluetoothApp";

	// Unique UUID for this application
	private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	// Member fields
	private final BluetoothAdapter btAdapter;
	private final Handler btHandler;
	private HiloServidor serverThread;
	private HiloCliente clientThread;
	private HiloConexion connectionThread;
	private int State;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////----------CONSTRUCTOR----------///////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor. Prepares a new BluetoothChat session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 */
	public BluetoothSerialService(Context context, Handler handler) {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		State = STATE_NONE;
		btHandler = handler;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////-------ESTABLECER ESTADO-------//////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Set the current state of the chat connection
	 * @param state  An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + State + " -> " + state);
		State = state;

		// Give the new state to the Handler so the UI Activity can update
		btHandler.obtainMessage(BluetoothList.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////-------GET ESTADO-------//////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Return the current connection state. */
	public synchronized int getState() {
		return State;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////---------SERVICIO---------/////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////--------INICIAR SERVICIO--------//////////////////////////////////////////
	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume() */
	public synchronized void start() {
		if (D) Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (clientThread != null) {clientThread.cancel(); clientThread = null;}

		// Cancel any thread currently running a connection
		if (connectionThread != null) {connectionThread.cancel(); connectionThread = null;}

		// Start the thread to listen on a BluetoothServerSocket
		if (serverThread == null) {
			serverThread = new HiloServidor();
			serverThread.start();
		}
		setState(STATE_LISTEN);
	}

	//////////////////////////////////////////-------SOLICITAR CONEXION-------//////////////////////////////////////////
	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * @param device  The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device) { // Hilo conector (Solicita conexión)
		if (D) Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (State == STATE_CONNECTING) {
			if (clientThread != null) {clientThread.cancel(); clientThread = null;}
		}

		// Cancel any thread currently running a connection
		if (connectionThread != null) {connectionThread.cancel(); connectionThread = null;}

		// Start the thread to connect with the given device
		clientThread = new HiloCliente(device);
		clientThread.start();
		setState(STATE_CONNECTING);
	}

	///////////////////////////////////////////-------REALIZAR CONEXION-------///////////////////////////////////////////
	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (D) Log.d(TAG, "connected");

		// Cancel the thread that completed the connection
		if (clientThread != null) {clientThread.cancel(); clientThread = null;}

		// Cancel any thread currently running a connection
		if (connectionThread != null) {connectionThread.cancel(); connectionThread = null;}

		// Cancel the accept thread because we only want to connect to one device
		if (serverThread != null) {serverThread.cancel(); serverThread = null;}

		// Start the thread to manage the connection and perform transmissions
		connectionThread = new HiloConexion(socket);
		connectionThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = btHandler.obtainMessage(BluetoothList.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothList.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		btHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}


	////////////////////////////////////////////////-------ENVIAR-------////////////////////////////////////////////////
	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * @param out The bytes to write
	 * @see HiloConexion#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		HiloConexion r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (State != STATE_CONNECTED) return;
			r = connectionThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	//////////////////////////////////////////--------FINALIZAR SERVICIO--------//////////////////////////////////////////
	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D) Log.d(TAG, "stop");
		if (clientThread != null) {clientThread.cancel(); clientThread = null;}
		if (connectionThread != null) {connectionThread.cancel(); connectionThread = null;}
		if (serverThread != null) {serverThread.cancel(); serverThread = null;}
		setState(STATE_NONE);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////----------FALLOS EN LA CONEXION----------//////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////-------CONEXION FALLIDA-------///////////////////////////////////////////
	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = btHandler.obtainMessage(BluetoothList.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothList.TOAST, "Unable to connect device");
		msg.setData(bundle);
		btHandler.sendMessage(msg);
	}

	///////////////////////////////////////////-------CONEXION PERDIDA-------///////////////////////////////////////////
	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = btHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothList.TOAST, "Device connection was lost");
		msg.setData(bundle);
		btHandler.sendMessage(msg);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////----------THREADS----------/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////-------HILO SERVIDOR-------/////////////////////////////////////////////
	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted
	 * (or until cancelled).
	 */
	private class HiloServidor extends Thread { // Hilo servidor
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public HiloServidor() {
			BluetoothServerSocket tmp = null; //Temp server socket

			// Create a new listening server socket
			try {
				tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread"); // Hilo servidor
			BluetoothSocket socket = null;
			setState(STATE_LISTEN);

			// Listen to the server socket if we're not connected
			while (State != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept(); // Cuando cliente solicite conexión
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (BluetoothSerialService.this) { //Lock
						switch (State) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							//acceptThread = new AcceptThread(socket);
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate new socket.
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
			if (D) Log.i(TAG, "END mAcceptThread");
		}

		public void cancel() {
			if (D) Log.d(TAG, "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}

	//////////////////////////////////////////////-------HILO CLIENTE-------//////////////////////////////////////////////
	/**
	 * This thread runs while attempting to make an outgoing connection
	 * with a device. It runs straight through; the connection either
	 * succeeds or fails.
	 */
	private class HiloCliente extends Thread { // Hilo cliente
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public HiloCliente(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			//  Esta parte funciona con HC-05
			Method m;
			try {
				m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
				tmp = (BluetoothSocket) m.invoke(device, 1);

			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} 
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			btAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
				setState(STATE_CONNECTING);
			} catch (IOException e) {
				connectionFailed();
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}
				setState(STATE_NONE);
				// Start the service over to restart listening mode
				BluetoothSerialService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothSerialService.this) {
				clientThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/////////////////////////////////////////////-------HILO CONEXION-------/////////////////////////////////////////////
	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	private class HiloConexion extends Thread { // Hillo Conexion
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public HiloConexion(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			setName(socket.getRemoteDevice().getName() + "["+ socket.getRemoteDevice().getAddress() +"]");

			// variables temporales
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			setState(STATE_CONNECTED);

			String message = "";
			char input;

			// Este método se ha modificado para que el Bluetooth reciba todo lo acumulado hata \n
			while (true) {
				try {
					input = (char)mmInStream.read();
					if(input == '\n') {
						Log.e("Mensaje", message);
						Message msg = Message.obtain();
						msg.obj = message;
						btHandler.obtainMessage(MESSAGE_READ,msg.obj).sendToTarget();                	   
						message="";
					} else {
						message += input;  	 
					}
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * @param buffer  The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				// Share the sent message back to the UI Activity
				btHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
