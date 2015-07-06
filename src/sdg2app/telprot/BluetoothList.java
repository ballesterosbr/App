package sdg2app.telprot;

import java.util.ArrayList;

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

	// Variables y elementos utilizados
	private Button buttonBT;
	private BluetoothAdapter btAdapter;
	private Button discButton;
	private ArrayList<BluetoothDevice> arrayDevices;
	private ListView listBTs;
	private TextView connexionState;
	private TextView textMsg;
	private TextView bTlist;
	private TextView phoneList;
	private Button sendButton;
	private Button deleteButton;
	private Button listButton;
	private EditText inputText;
	private TextView notice;
	private ListView phoneListview;
	private String [] phones;

	private static final String TAG = "BluetoothApp";

	// Tipos de mensaje a manejar por el Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// String recibidos por el Handler
	public static final String DEVICE_NAME = "deviceName";
	public static final String TOAST = "toast";

	// Nombre del dispositivo conectado
	private String connectedDeviceName = null;   
	
	// Adaptador Bluetooth local
	private BluetoothSerialService bluetoothService = null;
	private BluetoothDevice lastDevice;

	//////////////////////////////////////////------DEFINIR ELEMENTOS------//////////////////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bt_list);	

		arrayDevices = new ArrayList<BluetoothDevice>();

		discButton = (Button)findViewById(R.id.DiscButton);
		discButton.setOnClickListener(this);

		listBTs = (ListView)findViewById(R.id.ListBTs);
		phoneListview = (ListView)findViewById(R.id.phoneListview);
		
		inputText = (EditText)findViewById(R.id.message);

		notice = (TextView)findViewById(R.id.notice);
		bTlist = (TextView)findViewById(R.id.BTlist);
		phoneList = (TextView)findViewById(R.id.PhoneList);
		textMsg = (TextView)findViewById(R.id.textMsg);
		connexionState = (TextView)findViewById(R.id.connexionState);

		buttonBT = (Button) findViewById(R.id.BtButton);
		buttonBT.setOnClickListener(this);

		sendButton = (Button)findViewById(R.id.SendButton);
		sendButton.setOnClickListener(this);

		deleteButton =  (Button)findViewById(R.id.DeleteButton);
		deleteButton.setOnClickListener(this);

		listButton = (Button)findViewById(R.id.ListButton);
		listButton.setOnClickListener(this);

		eventsRegister();
		AdapterConfiguration();
		configurarLista();
		deviceListPulse();
		pulsadorListaTelefonos();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_bt_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!btAdapter.isEnabled()) {
		} else {
			if (bluetoothService == null) setupBluetoothApp();
		}
	}

	@Override
	protected synchronized void onResume() {
		super.onResume();

		if (!btAdapter.isEnabled()) {
		} else {
			if (bluetoothService == null) setupBluetoothApp();
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

	//////////////////////////////////////////////------BOTONES------//////////////////////////////////////////////
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.BtButton: 

			if(btAdapter.isEnabled()){
				listBTs.removeAllViewsInLayout();
				btAdapter.disable();
				bTlist.setText("");
				phoneList.setText("");
				textMsg.setText("");
				connexionState.setText("");
				inputText.getText().clear();
			}else{
				Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBT);
				break;
			}
		case R.id.DiscButton:{

			arrayDevices.clear();
			phoneListview.removeAllViewsInLayout();
			btAdapter.startDiscovery();

			if(btAdapter.isDiscovering())
				btAdapter.cancelDiscovery();

			if(!arrayDevices.isEmpty()){
				listBTs.removeAllViewsInLayout();
			}

			if(btAdapter.startDiscovery()){
				bTlist.setText(R.string.bt_list);
				Toast.makeText(this, "Iniciando búsqueda de dispositivos", Toast.LENGTH_SHORT).show();
			}else
				Toast.makeText(this, "Bluetooth desactivado", Toast.LENGTH_SHORT).show();
			break;
		}

		case R.id.ListButton:

			phoneList.setText(R.string.phone_list);	
			sendMessage("LIST");
			textMsg.setText("Lista de teléfonos actualizada");
			Log.e(TAG, "LIST");
			break;

		case R.id.DeleteButton:

			sendMessage("DELETE_"+ inputText.getText().toString());
			textMsg.setText("Se ha eliminado el teléfono: "+ inputText.getText().toString());
			sendMessage("LIST");
			textMsg.setText("Lista de teléfonos actualizada");
			Log.e(TAG, inputText.getText().toString());
			break;

		case R.id.SendButton:

			sendMessage("ADD_" + inputText.getText().toString());
			textMsg.setText("Se ha añadido el teléfono: " + inputText.getText().toString());
			sendMessage("LIST");
			textMsg.setText("Lista de teléfonos actualizada");
			Log.e(TAG, inputText.getText().toString());
			break;
		}
	}

	////////////////////////////////////////------CONFIGURACION INICIAL------////////////////////////////////////////
	private void AdapterConfiguration(){
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		disableElements();

		if(btAdapter.isEnabled()){
			buttonBT.setText(R.string.deactivate);
			setVisible();
		}else{
			setInvisible();
			notice.setVisibility(View.VISIBLE);
			buttonBT.setText(R.string.activate);
		}
	}

	//////////////////////////////////////////------PULSAR EN LIST TELEFONOS------//////////////////////////////////////////
	private void pulsadorListaTelefonos(){

		phoneListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {		    	
				// No se ha añadido ninguna opción para interactuar con la lista
			}
		}); 
	}


	//////////////////////////////////////////------PULSAR EN LISTVIEW------//////////////////////////////////////////
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

	//////////////////////////////////////------DIALOGO DE CONEXION A BT------///////////////////////////////////////
	private AlertDialog crearDialogoConexion(String titulo, String mensaje, final String direccion)
	{
		// Se asocia titulo y mensaje al constructor del Alertdialog
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(titulo);
		alertDialogBuilder.setMessage(mensaje);

		// Se crea un OnclickListener para conectar con el dispositivo
		DialogInterface.OnClickListener listenerOk = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				connectDevice(direccion);
			}
		};

		// Se crea un OnClickListener para cancelar en caso de equivocación
		DialogInterface.OnClickListener listenerCancelar = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		};

		// Se asignan los botones a las distintas acciones que tenemos 
		alertDialogBuilder.setPositiveButton(R.string.connect, listenerOk);
		alertDialogBuilder.setNegativeButton(R.string.cancel, listenerCancelar);

		return alertDialogBuilder.create();
	}

	///////////////////////////////////////////////------HANDLER------///////////////////////////////////////////////
	// Handler maneja los distintos tipos de eventos (mensajes y estados) y actúa según ellos
	private final Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {

			byte[] buffer = null;
			String mensaje = null;
			String message = null;

			switch(msg.what){
			case MESSAGE_STATE_CHANGE:
				switch(msg.arg1){
				case BluetoothSerialService.STATE_CONNECTED:
					connexionState.setText(R.string.connectedTo);
					connexionState.append(" ");
					connexionState.append(connectedDeviceName);
					enableElements();
					break;
				case BluetoothSerialService.STATE_CONNECTING:
					connexionState.setText(R.string.connecting);
					disableElements();
					break;
				case BluetoothSerialService.STATE_LISTEN:
					disableElements();
					break;
				case BluetoothSerialService.STATE_NONE:
					connexionState.setText(R.string.not_connected);
					break;
				}
				break;
				
			case MESSAGE_WRITE: 
				buffer = (byte[])msg.obj;
				mensaje = new String(buffer);
				mensaje = getString(R.string.send) + ": " + mensaje;				
				Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
				break;
				
			case MESSAGE_READ:      
				connexionState.setVisibility(View.VISIBLE);
				message = (String)msg.obj;
				mensaje = new String(message);

				if(mensaje.contains(":")){
					phones = mensaje.split(":");
					ArrayAdapter<String> adaptador = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, phones);
					phoneListview.setAdapter(adaptador);
				}

				Log.e("mensaje convertido", mensaje);
				textMsg.setText("Lista de teléfonos actualizada");
				break;
				
			case MESSAGE_DEVICE_NAME:
				connectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Conectado a "+ connectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
				
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
				
			default: break;
			}
		}		
	};

	//////////////////////////////////////////------CONECTAR A DISPOSITIVO------//////////////////////////////////////////
	// Metodo para conectarse a dispositivo
	public void connectDevice(String direccion){
		Toast.makeText(this, "conectando a: "+ direccion, Toast.LENGTH_LONG).show();
		if(bluetoothService != null){
			BluetoothDevice dispositivoRemoto = btAdapter.getRemoteDevice(direccion);
			bluetoothService.connect(dispositivoRemoto);
			this.lastDevice = dispositivoRemoto;
		}
	}

	//////////////////////////////////////////////------ENVIAR MENSAJE------//////////////////////////////////////////////
	public void sendMessage(String message){
		Log.e(TAG, "Llega a enviar");
		Log.e(TAG, message);
		if(bluetoothService.getState() != BluetoothSerialService.STATE_CONNECTED){
			Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		if(message.length() > 0){
			byte[] send = message.getBytes();
			bluetoothService.write(send);
		}
	}

	//////////////////////////////////////////------DESCUBRIR DISPOSITIVOS------//////////////////////////////////////////
	private final BroadcastReceiver Receiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent){
			final String action = intent.getAction();

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
				final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch(estado){
				case BluetoothAdapter.STATE_ON:{
					buttonBT.setText(R.string.deactivate);
					setVisible();
					break;
				}
				case BluetoothAdapter.STATE_OFF:{
					buttonBT.setText(R.string.activate);
					setInvisible();
					break;
				}
				default: break;
				}
			}

			else if(BluetoothDevice.ACTION_FOUND.equals(action)){

				if(arrayDevices == null){
					arrayDevices = new ArrayList<BluetoothDevice>();
				}
				BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				arrayDevices.add(dispositivo);
			}

			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){				
				ArrayAdapter arrayAdapter = new BluetoothDeviceArrayAdapter(getBaseContext(), android.R.layout.simple_list_item_2,arrayDevices);
				listBTs.setAdapter(arrayAdapter);		
			}
		}
	};

	//////////////////////////////////////////------INICIAR SERVICIO BLUETOOTH------//////////////////////////////////////////
	private void setupBluetoothApp() {
		// Inicializa el servicio Bluetooth para crear conexiones
		bluetoothService = new BluetoothSerialService(this, mHandler);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void eventsRegister(){
		IntentFilter filtro = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		filtro.addAction(BluetoothDevice.ACTION_FOUND);
		filtro.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

		this.registerReceiver(Receiver, filtro);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////-------VISIBILIDAD ELEMENTOS-------/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	////////////////////////////////////////////------OCULTAR ELEMENTOS------////////////////////////////////////////////

	private void setInvisible(){
		connexionState.setVisibility(View.GONE);
		sendButton.setVisibility(View.GONE);
		inputText.setVisibility(View.GONE);
		discButton.setVisibility(View.GONE);
		listBTs.setVisibility(View.GONE);
		deleteButton.setVisibility(View.GONE);
		listButton.setVisibility(View.GONE);
		phoneListview.setVisibility(View.GONE);
		phoneList.setVisibility(View.GONE);
		notice.setVisibility(View.VISIBLE);
	}

	////////////////////////////////////////////------APARECER ELEMENTOS------///////////////////////////////////////////
	private void setVisible(){
		connexionState.setVisibility(View.VISIBLE);
		sendButton.setVisibility(View.VISIBLE);
		inputText.setVisibility(View.VISIBLE);
		discButton.setVisibility(View.VISIBLE);
		listBTs.setVisibility(View.VISIBLE);
		deleteButton.setVisibility(View.VISIBLE);
		listButton.setVisibility(View.VISIBLE);
		phoneListview.setVisibility(View.VISIBLE);
		phoneList.setVisibility(View.VISIBLE);
		notice.setVisibility(View.GONE);
	}

	////////////////////////////////////////////------HABILITAR ELEMENTOS------///////////////////////////////////////////
	private void enableElements(){
		sendButton.setEnabled(true);
		deleteButton.setEnabled(true);
		inputText.setEnabled(true);
		listButton.setEnabled(true);
	}

	////////////////////////////////////////////------DESHABILITAR ELEMENTOS------///////////////////////////////////////////
	private void disableElements(){
		sendButton.setEnabled(false);
		deleteButton.setEnabled(false);
		inputText.setEnabled(false);
		listButton.setEnabled(false);
	}

	////////////////////////////////////////////------CONFIGURAR LISTA------/////////////////////////////////////////////
	private void configurarLista(){
		listBTs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView adapter, View view,	int position, long arg) {
				BluetoothDevice dispositivo = (BluetoothDevice)listBTs.getAdapter().getItem(position);
			}
		});	
	}
}
