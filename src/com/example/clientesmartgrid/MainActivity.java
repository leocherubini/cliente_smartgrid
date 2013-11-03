/*
 * Esta aplicacao tem como objetivo apresentar um exemplo de cliente
 * na comunicacao com o projeto servidor_smartgrid usando o protocolo bluetooth.
 * Este presente trabalho busca complementar um projeto de Smart Grid que simula
 * uma aplicação de monitoramento de veiculos.
 * Em termos gerais, o servidor sera apresentado neste trabalho como o projeto
 * servidor_smartgrid, sendo este uma aplicacaoo Java que vai monitorar os veiculos
 * a partir do protocolo bluetooth. Ja esta presente aplicacao e um projeto Android
 * que representara o veiculo monitorado pela aplicacao servidor.
 * 
 * Esta aplicacao Android e responsavel por enviar um arquivo via protocolo bluetooth
 * para a aplicacao servidor_smartgrid.
 * 
 * Autor: Leonardo Fernandes Cherubini.
 * Instituicao: IFMT - PET AutoNet.
 * 
 * class MainActivity
 * date 02/11/2013
 * copyright Cherubini
 */

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
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	/* Constante responsavel por identificar a Intent do arquivo */
	private static final int REQUEST_FILE = 1;
	
	/* Constante responsavel por identificar a Intent do dispositivo  */
	private static final int REQUEST_DEVICE = 2;
	
	/* Constante responsavel por identificar a Intent do adaptador bluetooth */
	private static final int REQUEST_ADAPTOR = 3;
	
	/* Variavel responsavel por armazenar o caminho do arquivo selecionado */
	private String caminhoArquivo;
	
	/* Variavel responsavel por armazenar o endereco mac do dispositivo selecionado */
	private String address;
	
	/* Componente responsavel por exibir o caminho do arquivo */
	private TextView arquivoSelecionado;
	
	/* Componente responsavel por exibir o nome do servidor */
	private TextView nomeServidor;
	
	/* Componente responsavel por exibir o endereco do servidor */
	private TextView enderecoServidor;
	
	/* Componente responsavel por exibir o nome do arquivo */
	private TextView arquivo;
	
	/* Botao de pesquisa do arquivo */
	private Button botaoPesquisarArquivo;
	
	/* Botao de pesquisa por um dispositivo externo */
	private Button botaoPesquisarServidor;
	
	/* Botao de responsavel por enviar arquivos para o servidor */
	private Button botaoEnviar;
	
	/* Variavel responsavel por representar o adaptador bluetooth do presente dispositivo */
	private BluetoothAdapter adaptador;
	
	/* Variavel responsavel por representar o dispositivo servidor */
	private BluetoothDevice device;
	
	/* Socket da conexao */
	private BluetoothSocket socket;
	
	/* Fluxo de saida de dados */
	private DataOutputStream output;
	
	/* Intent responsavel pelo acesso a tela de pesquisa */
	private Intent it;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* Ligando o adaptador bluetooth */
		ligarAdaptador();
		
		/* Inicializando os componentes */
		initCompenents();

		/* Inicializando os eventos dos componentes */
		initListeners();
	}

	/**
	 * Metodo responsavel pela inicializacao dos componentes
	 * @return void
	 */
	private void initCompenents() {
		// inicializando a widget do caminho do arquivo
		arquivoSelecionado = (TextView) findViewById(R.id.texto03);
		// inicializando a widget do nome do servidor
		nomeServidor = (TextView) findViewById(R.id.texto06);
		// inicializando a widget do endereco do servidor
		enderecoServidor = (TextView) findViewById(R.id.texto08);
		// inicializando a widget do nome do arquivo
		arquivo = (TextView) findViewById(R.id.texto10);

		// inicializando o botao pesquisar arquivo
		botaoPesquisarArquivo = (Button) findViewById(R.id.botao_arquivo);
		// inicializando o botao pesquisar servidor
		botaoPesquisarServidor = (Button) findViewById(R.id.botao_servidor);
		// inicializando o botao de enviar arquivo
		botaoEnviar = (Button) findViewById(R.id.botao_enviar);
		
		// inicializando a variavel do dispositivo servidor
		device = null;
		// inicializando a varialvel do endereco mac do servidor
		address = null;
		// inicializando o Intent da tela de pesquisa
		it = new Intent(this, TelaPesquisa.class);
	}

	 /**
	  * Metodo responsavel pela inicializacao dos eventos dos componentes
	  * @return void
	  */
	private void initListeners() {
		// configurando o evento do botao de pesquisa do arquivo
		botaoPesquisarArquivo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Intent configurada para acessar arquivos do presente dispositivo
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				// configurando o tipo de arquivo a ser selecionado
				intent.setType("file/*");
				// inicializando a pesquisa de um arquivo
				startActivityForResult(intent, REQUEST_FILE);
			}
		});
		
		// configurando o evento do botao de pesquisa do servidor
		botaoPesquisarServidor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// inicializando a tela de pesquisa
				startActivityForResult(it, REQUEST_DEVICE);
			}
		});
		
		// configurando o evento do botao de envio de arquivo 
		botaoEnviar.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// variavel do tipo File responsavel por armazenar o arquivo selecionado
				File file = new File(caminhoArquivo);
				// inicializando a classe Enviar com a variavel socket e o arquivo file
				new Enviar(socket, file).start();
			}
		});
	}
	
	/**
	  * Metodo responsavel por ligar o adaptador bluetooth se caso este estiver desligado
	  * @return void
	  */
	private void ligarAdaptador() {
		// variavel sendo referenciada ao adaptador do presente dispositivo
		adaptador = BluetoothAdapter.getDefaultAdapter();
		// estrutura de decisao para ligar o Bluetooth
		if (!adaptador.isEnabled()) {

			/*
			 * Se o Radio Bluetooth estiver desligado, o BluetoothAdapter
			 * solicitara a permissao do usuario para ligar o Bluetooth
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
	
	/**
	 * Método nativo da API Android responsavel por retornar dados de outras Activities
	 * @return void
	 */
	protected void onActivityResult(int codigo, int resultado, Intent it) {
		// a estrutura vai verificar qual a Activity chamou este metodo
		// usando a constante de identificacao
		switch (codigo) {
		case REQUEST_FILE: // identificacao da Activity de pesquisa de arquivo
			if (resultado == RESULT_OK) {
				// recupera o caminho total do arquivo
				caminhoArquivo = it.getData().getPath();
				
				// recupera o nome do arquivo
				String nomeArquivo = it.getData().getLastPathSegment();

				// atribui o caminho ao respectiva widget na tela
				arquivoSelecionado.setText(caminhoArquivo);
				// configura a widget com a cor verde
				arquivoSelecionado.setTextColor(Color.GREEN);
				// configura o nome do arquivo na respectiva widget na tela
				arquivo.setText(nomeArquivo);
			}
			break;
			
		case REQUEST_DEVICE: // identificacao da Activity de pesquisa do servidor
			if (it != null) {
				// recupera o endereco MAC do servidor
				address = it.getExtras().getString("msg");
				
				// armazena o dispositivo do servidor
				device = adaptador.getRemoteDevice(address);
				
				// configura a widget do endereco do servidor na tela
				enderecoServidor.setText(address);
				// cofigura a cor da widget para verde
				enderecoServidor.setTextColor(Color.GREEN);
				
				// configura a widget do nome do servidor
				nomeServidor.setText(device.getName());
				// configura a cor da widget para verde
				nomeServidor.setTextColor(Color.GREEN);
				// inicializa a classe de conexao com o servidor
				new ConnectThread(device).start();
				
			}
			break;
		}
	}
	
	private class ConnectThread extends Thread {

		// variavel final que armazenarao dispositivo escolhido para a conexao
		private final BluetoothDevice mmDevice;

		// Construtor do ConnectThread
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			try {

				// variavel socket recebe um identificador unico UUID
				tmp = device.createRfcommSocketToServiceRecord(UUID
						.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = tmp;
		}

		public void run() {
			// Cancela a pesquisa de dispositivos externos para economizar banda
			adaptador.cancelDiscovery();

			try {
				// Socket conecta a aplicativo Android no dispositivo externo
				socket.connect();
				alerta("Conectado");
			} catch (IOException e) {
				try {
					socket.close();
					alerta("NÃ£o Conectado");
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
	
	/* Metodos responsaveis para o envio de uma mensagem na tela */
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
	
	/**
	 * Classe responsavel por enviar o arquivo ao servidor.
	 * @author CHERUBINI
	 */
	public class Enviar extends Thread {

		private BluetoothSocket mmSocket;
		private DataOutputStream mmOutStream;
		private File file;

		public Enviar(BluetoothSocket socket, File file) {
			mmSocket = socket;
			this.file = file;
			DataOutputStream tmpOut = null;

			try {
				tmpOut = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			mmOutStream = tmpOut;
		}

		public void run() {
			int total = 0;
			try {
				// inicializando o fluxo de envio de dados
				output = new DataOutputStream(socket.getOutputStream());
				// enviando o nome do arquivo
				output.writeUTF(file.getName());
				// enviando o tamanho do arquivo para o buffer do servidor
				output.writeLong(file.length());
				// enviando o tamanho do arquivo para o textArea do servidor
				output.writeLong(file.length());
				
				// fuxo de entrada do arquivo
				FileInputStream in = new FileInputStream(file);

				byte[] buf = new byte[4096];

				while (true) {
					int len = in.read(buf);

					if (len == -1)
						break;
					total = total + len;
					
					output.write(buf, 0, len);
				}
				output.close(); // limpa o fluxo
				socket.close(); // fecha o socket

			} catch (Exception erro) {
				alerta("Erro transferencia");
			}
		}

	}
}
