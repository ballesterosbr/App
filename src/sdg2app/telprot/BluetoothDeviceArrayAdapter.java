package sdg2app.telprot;

import java.util.List;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BluetoothDeviceArrayAdapter extends ArrayAdapter{

	private List<BluetoothDevice> deviceList;
	private Context context;

	/////////////////////////////////////////----ADAPTADOR PARA EL LISTVIEW DISPOSITIVOS---////////////////////////////////////////////
	public BluetoothDeviceArrayAdapter(Context context, int textViewResourceId, List<BluetoothDevice> objects){
		super(context, textViewResourceId, objects);

		this.deviceList = objects;
		this.context = context;
	}

	// Contador para saber los elementos y así las posiciones.
	@Override
	public int getCount(){
		if(deviceList != null){
			return deviceList.size();
		}else{
			return 0;
		}
	}

	// Selecciona item a partir de la posición
	@Override
	public Object getItem(int position){
		return(deviceList == null ? null : deviceList.get(position));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		if((deviceList == null) || (context == null))
			return null;

		// Con un Layout inflater creamos la vista
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Usamos simple_list_item2 porque permite dos renglones (dos textview).
		// Text1 será para el nombre del dispositivo
		// Text2 será para la dirección física del dispositivo
		View vista = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
		TextView nombreDispositivo = (TextView)vista.findViewById(android.R.id.text1);
		TextView direccionDispositivo = (TextView)vista.findViewById(android.R.id.text2);

		// Una vez tenemos el dispositivo a partir de su posición, le asociamos nombre y direccion
		BluetoothDevice dispositivo = (BluetoothDevice)getItem(position);
		if(dispositivo != null)
		{
			nombreDispositivo.setText(dispositivo.getName());
			direccionDispositivo.setText(dispositivo.getAddress());
		}
		else
		{
			nombreDispositivo.setText("ERROR");
		}

		// return de los dos Textview
		return vista;
	}
}
