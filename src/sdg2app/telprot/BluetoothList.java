package sdg2app.telprot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothList extends Activity implements View.OnClickListener{

	private Button botonBT;
	private BluetoothAdapter BtAdapter;
	private Button discButton;
	private ArrayList<BluetoothDevice> arrayDevices;
	private ListView listBTs;
	
	// private layouts
	private TextView mTitle;
	private TextView textMsg;
	private TextView BTlist;
	private Button SendButton;
	private EditText mensaje;

    private static final String TAG = "BluetoothChat";
	
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;   
    // Local Bluetooth adapter
    // Member object for the chat services
    private BluetoothSerialService mChatService = null;
    private BluetoothDevice ultimoDispositivo;
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bt_list);	
		
		arrayDevices = new ArrayList<BluetoothDevice>();
		
		discButton = (Button)findViewById(R.id.DiscButton);
		discButton.setOnClickListener(this);
		
		listBTs = (ListView)findViewById(R.id.ListBTs);
		
		mensaje = (EditText)findViewById(R.id.mensaje);
		
		BTlist = (TextView)findViewById(R.id.BTlist);
		textMsg = (TextView)findViewById(R.id.textMsg);
		
		mTitle = (TextView)findViewById(R.id.mTitle);
		botonBT = (Button) findViewById(R.id.BtButton);
		botonBT.setOnClickListener(this);
		
		SendButton = (Button)findViewById(R.id.SendButton);
		SendButton.setOnClickListener(this);
		
		eventsRegister();
		AdapterConfiguration();
		configurarLista();
		deviceListPulse();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_bt_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!BtAdapter.isEnabled()) {
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
		
	}

	@Override
	protected synchronized void onResume() {
		super.onResume();
		
		if (!BtAdapter.isEnabled()) {
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }

	}

	@Override
	protected synchronized void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.BtButton: 
			
			if(BtAdapter.isEnabled()){
//				arrayDevices.clear();
				listBTs.removeAllViewsInLayout();
				BtAdapter.disable();
				botonBT.setText(R.string.activate);
				BTlist.setText("");
//				SendButton.setEnabled(false);
				textMsg.setText("");
				mTitle.setText("");
				mensaje.getText().clear();
			}else{
				Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBT);
				botonBT.setText(R.string.deactivate);
//				SendButton.setEnabled(true);
			
			break;
		}
		case R.id.DiscButton:{
			
			arrayDevices.clear();
			BtAdapter.startDiscovery();
			
			if(BtAdapter.isDiscovering())
				BtAdapter.cancelDiscovery();
							
			if(!arrayDevices.isEmpty()){
				listBTs.removeAllViewsInLayout();
			}
			
			if(BtAdapter.startDiscovery()){
				BTlist.setText(R.string.bt_list);
				Toast.makeText(this, "Iniciando búsqueda de dispositivos", Toast.LENGTH_SHORT).show();
			}else
				Toast.makeText(this, "Bluetooth desactivado", Toast.LENGTH_SHORT).show();
			break;
		}
		case R.id.SendButton:
			sendMessage(mensaje.getText().toString());
			textMsg.setText("El mensaje enviado es: " + mensaje.getText().toString());
			Log.e(TAG, mensaje.getText().toString());
		}
	}
	
	private void AdapterConfiguration(){
		BtAdapter = BluetoothAdapter.getDefaultAdapter();
				
		SendButton.setEnabled(false);
		if(BtAdapter == null){
			botonBT.setEnabled(false);
			return;
		}
				
		if(BtAdapter.isEnabled()){
			botonBT.setText(R.string.deactivate);
//			SendButton.setEnabled(true);
		}else{
//			SendButton.setEnabled(false);
			botonBT.setText(R.string.activate);
		}
	}
	
	// Método para pulsar en la listview
	private void deviceListPulse(){
		listBTs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView adapter, View view, int position, long id) {
				BluetoothDevice dispositivo = (BluetoothDevice)listBTs.getAdapter().getItem(position);
				AlertDialog dialog = crearDialogoConexion(getString(R.string.connect), "Conectar a "+ dispositivo.getName(), dispositivo.getAddress());
				dialog.show();	
			}
		});
	}
	
	private AlertDialog crearDialogoConexion(String titulo, String mensaje, final String direccion)
    {
    	// Instanciamos un nuevo AlertDialog Builder y le asociamos titulo y mensaje
    	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(titulo);
		alertDialogBuilder.setMessage(mensaje);

		// Creamos un nuevo OnClickListener para el boton OK que realice la conexion
		DialogInterface.OnClickListener listenerOk = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				connectDevice(direccion);
			}
		};
		
		// Creamos un nuevo OnClickListener para el boton Cancelar
		DialogInterface.OnClickListener listenerCancelar = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		};
		
		// Asignamos los botones positivo y negativo a sus respectivos listeners 
		alertDialogBuilder.setPositiveButton(R.string.connect, listenerOk);
		alertDialogBuilder.setNegativeButton(R.string.cancel, listenerCancelar);
		
		return alertDialogBuilder.create();
    }
	
	
	// Handler Maneja los distintos tipos de eventos (mensajes y estados) y actúa según ellos
	private final Handler mHandler = new Handler(){
		
		@Override
        public void handleMessage(Message msg) {
			
			byte[] buffer = null;
			String mensaje = null;
			
			switch(msg.what){
			case MESSAGE_STATE_CHANGE:
				switch(msg.arg1){
				case BluetoothSerialService.STATE_CONNECTED:
					mTitle.setText(R.string.connectedTo);
					mTitle.append(" ");
					SendButton.setEnabled(true);
					mTitle.append(mConnectedDeviceName);
					break;
				case BluetoothSerialService.STATE_CONNECTING:
					mTitle.setText(R.string.connecting);
					SendButton.setEnabled(false);
					break;
				case BluetoothSerialService.STATE_LISTEN:
					SendButton.setEnabled(false);
					break;
				case BluetoothSerialService.STATE_NONE:
					mTitle.setText(R.string.not_connected);
					  break;
                }
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_READ:                
                break;
            case MESSAGE_DEVICE_NAME:
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Conectado a "+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
       
//				case BluetoothSerialService.MESSAGE_READ: {
//					buffer = (byte[])msg.obj;
//					mensaje = new String(buffer, 0, msg.arg1);
//					textMsg.setText(mensaje);
//					break;
//				}
//				case
//				}
//				case MESSAGE_STATE_CHANGE:{
//					
//					switch(msg.arg1){
//					
//					case BluetoothSerialService.STATE_NONE:{
//	                    mTitle.setText(R.string.not_connected);
//						break;	
//					}
//					case BluetoothSerialService.STATE_LISTEN:
//					case BluetoothSerialService.STATE_CONNECTING:{
//						mensaje = getString(R.string.connected) + " " + mChatService.getState();
//						Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
//						mTitle.setText(mensaje);
//						mTitle.append(mConnectedDeviceName);
//						break;	
//					}
//					case BluetoothSerialService.STATE_CONNECTED:{
//						
//						
//						mensaje = getString(R.string.connected) + " " + mChatService.getState();
//						Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
//						mTitle.setText(mensaje);
//						mTitle.append(mConnectedDeviceName);
//						break;						
//					}
//					}
//					break;
//				}
//				case BluetoothSerialService.MESSAGE_DEVICE_NAME:{
//					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//					break;
//				}
//				case BluetoothSerialService.MESSAGE_TOAST:{
//					break;
//			      }
//	        }
	    
	    // Metodo para conectarse a dispositivo
	    public void connectDevice(String direccion){
	    	Toast.makeText(this, "conectando a: "+ direccion, Toast.LENGTH_LONG).show();
	    	if(mChatService != null){
	    		BluetoothDevice dispositivoRemoto = BtAdapter.getRemoteDevice(direccion);
	    		mChatService.connect(dispositivoRemoto);
	    		this.ultimoDispositivo = dispositivoRemoto;
	    	}
	    }
	    
	    // Enviar mensaje
	    public void sendMessage(String message){
	    	Log.e(TAG, "Llega a enviar");
	    	Log.e(TAG, message);
	    	if(mChatService.getState() != BluetoothSerialService.STATE_CONNECTED){
	    		Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
	    		return;
	    	}
	    	
	    	if(message.length() > 0){
	    		byte[] send = message.getBytes();
	    		mChatService.write(send);
	    	}
	    }
	    
	
	// Descubrir dispositivos
	
	private final BroadcastReceiver Receiver = new BroadcastReceiver(){
		
		@Override
		public void onReceive(Context context, Intent intent){
			final String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
								
				if(arrayDevices == null){
					arrayDevices = new ArrayList<BluetoothDevice>();
				}
				BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				arrayDevices.add(dispositivo);
			}
			
//			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
//				final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//				switch(estado){
//				case BluetoothAdapter.STATE_ON:{
//					SendButton.setEnabled(true);
//					botonBT.setText(R.string.deactivate);
//					break;
//				}
//				case BluetoothAdapter.STATE_OFF:{
//					((Button)findViewById(R.id.BtButton)).setText(R.string.activate);
//					SendButton.setEnabled(false);
//					botonBT.setText(R.string.activate);
//					break;
//				}
//				default: break;
//				}
//			}
			
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){				
				ArrayAdapter arrayAdapter = new BluetoothDeviceArrayAdapter(getBaseContext(), android.R.layout.simple_list_item_2,arrayDevices);
				listBTs.setAdapter(arrayAdapter);		
		}
	}
	};

	private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothSerialService(this, mHandler);
    }
	
	private void eventsRegister(){
		IntentFilter filtro = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filtro.addAction(BluetoothDevice.ACTION_FOUND);
		filtro.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		
		this.registerReceiver(Receiver, filtro);
	}
	
	private void configurarLista(){
		listBTs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView adapter, View view,	int position, long arg) {
				BluetoothDevice dispositivo = (BluetoothDevice)listBTs.getAdapter().getItem(position);
			}
		});
		
	}
}