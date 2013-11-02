package com.example.clientesmartgrid;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int REQUEST_FILE = 1;
	private static final int REQUEST_DEVICE = 2;
	private static final int REQUEST_ADAPTOR = 3;
	private String caminhoArquivo;
	private String address;
	private TextView arquivoSelecionado;
	private TextView nomeServidor;
	private TextView enderecoServidor;
	private TextView arquivo;
	private Button botaoPesquisarArquivo;
	private Button botaoPesquisarServidor;
	private Button botaoEnviar;
	private BluetoothAdapter adaptador;
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private DataOutputStream output;
	private Intent it;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ligarAdaptador();
		
		initCompenents();

		initListeners();
	}

	private void initCompenents() {
		arquivoSelecionado = (TextView) findViewById(R.id.texto03);
		nomeServidor = (TextView) findViewById(R.id.texto06);
		enderecoServidor = (TextView) findViewById(R.id.texto08);
		arquivo = (TextView) findViewById(R.id.texto10);

		botaoPesquisarArquivo = (Button) findViewById(R.id.botao_arquivo);
		botaoPesquisarServidor = (Button) findViewById(R.id.botao_servidor);
		botaoEnviar = (Button) findViewById(R.id.botao_enviar);
		
		device = null;
		address = null;
		it = new Intent(this, TelaPesquisa.class);
	}

	private void initListeners() {
		botaoPesquisarArquivo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("file/*");
				startActivityForResult(intent, REQUEST_FILE);
			}
		});
		
		botaoPesquisarServidor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivityForResult(it, REQUEST_DEVICE);
			}
		});
		
		botaoEnviar.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				File file = new File(caminhoArquivo);
				new Enviar(socket, file).start();
			}
		});
	}
	
	private void ligarAdaptador() {
		adaptador = BluetoothAdapter.getDefaultAdapter();
		/* Estrutura de decis�o para ligar o Bluetooth */
		if (!adaptador.isEnabled()) {

			/*
			 * Se o R�dio Bluetooth estiver desligado BluetoothAdapter
			 * solicitar� a permiss�o do usu�rio para ligar o Bluetooth
			 */
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ADAPTOR);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	protected void onActivityResult(int codigo, int resultado, Intent it) {
		switch (codigo) {
		case REQUEST_FILE:
			if (resultado == RESULT_OK) {

				caminhoArquivo = it.getData().getPath();
				
				String nomeArquivo = it.getData().getLastPathSegment();

				arquivoSelecionado.setText(caminhoArquivo);
				arquivoSelecionado.setTextColor(Color.GREEN);
				
				arquivo.setText(nomeArquivo);

			}
			break;
			
		case REQUEST_DEVICE:
			if (it != null) {
				address = it.getExtras().getString("msg");

				device = adaptador.getRemoteDevice(address);
				
				enderecoServidor.setText(address);
				enderecoServidor.setTextColor(Color.GREEN);
				
				nomeServidor.setText(device.getName());
				nomeServidor.setTextColor(Color.GREEN);
				new ConnectThread(device).start();
				
			}
			break;
		}
	}
	
	private class ConnectThread extends Thread {

		/* Vari�vel final que armazenar� o dispositivo escolhido para a conex�o */
		private final BluetoothDevice mmDevice;

		/* Construtor do ConnectThread */
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			try {

				/* Vari�vel socket recebe um identificador �nico UUID */
				tmp = device.createRfcommSocketToServiceRecord(UUID
						.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = tmp;
		}

		public void run() {
			/* Cancela a pesquisa de dispositivos externos para economizar banda */
			adaptador.cancelDiscovery();

			try {
				/* Socket conecta a aplicativo Android no dispositivo externo */
				socket.connect();
				alerta("Conectado");
			} catch (IOException e) {
				try {
					socket.close();
					alerta("Não Conectado");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return;
			}

		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*M�todos respons�veis para o envio de uma mensagem na tela*/
	private final Handler h = new Handler() {
		public void handleMessage(Message msg) {
			String content = (String) msg.obj;
			Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT)
					.show();
		}
	};

	public void alerta(String message) {
		Message m = h.obtainMessage();
		m.obj = message;
		h.sendMessage(m);
	}
	
	public class Enviar extends Thread {

		private BluetoothSocket mmSocket;
		private DataOutputStream mmOutStream;
		private File file;

		public Enviar(BluetoothSocket socket, File file) {
			mmSocket = socket;
			this.file = file;
			DataOutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpOut = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			mmOutStream = tmpOut;
		}

		public void run() {
			// File log = new
			// File(Environment.getExternalStorageDirectory(),"teste02.mp3");
			int total = 0;
			try {

				output = new DataOutputStream(socket.getOutputStream());

				output.writeUTF(file.getName());
				//output.writeUTF(file.getAbsolutePath());
				output.writeLong(file.length());
				output.writeLong(file.length());
				
				FileInputStream in = new FileInputStream(file);

				byte[] buf = new byte[4096];

				while (true) {
					int len = in.read(buf);

					if (len == -1)
						break;
					total = total + len;
					//setTextoParametro(""+total);
					
					output.write(buf, 0, len);
				}
				///textoParametro.setText("Tamanho Total do arquivo: "+total);
				output.close(); // limpa o fluxo
				socket.close(); // fecha o socket

			} catch (Exception erro) {
				alerta("Erro transferencia");
			}
		}

	}
}
