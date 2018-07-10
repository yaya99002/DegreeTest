package com.example.degreetest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private TextView tVaue, hVaue;
	private EditText Ed_IP, Ed_post;
	private Button btnDegree;
	private Button btnFanclose;
	private Button btnFanopen;

	private String IP;
	private int port;
	private boolean timeON=false;//��ť��״̬
	MsgHandler handler; //UI����
	GetDegreeThread gThread = null; //��ȡ���ݵ��߳�
	SendCmdThread   gSendCmdThread=null;
	private final String MY_TAG = "MY_TAG";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tVaue = (TextView) findViewById(R.id.Temperature_text);
		hVaue = (TextView) findViewById(R.id.humidity_text);
		Ed_IP = (EditText) findViewById(R.id.etIp);
		Ed_post = (EditText) findViewById(R.id.etPort);
		handler = new MsgHandler(this);  //��ʼ��handler
		btnDegree = (Button) findViewById(R.id.btnDegree);
		btnFanclose = (Button) findViewById(R.id.fanclose);
		btnFanopen  = (Button) findViewById(R.id.fanopen);
		
		btnFanopen.setOnClickListener(new View.OnClickListener() {
		     public void  onClick(View v){					
		    	 if (!Ed_IP.getText().toString().equals("")
						&& !Ed_post.getText().toString().equals("")) {//�ж�������Ƿ�Ϊ�գ������Ϊ�վͽ���
					IP = Ed_IP.getText().toString();
					port = Integer.parseInt(Ed_post.getText().toString());//��StringתΪint
		    	 }
		    	 toSendCmd("open");
		     }
		});
		
		btnFanclose.setOnClickListener(new View.OnClickListener() {
		     public void  onClick(View v){
		    	 if (!Ed_IP.getText().toString().equals("")
							&& !Ed_post.getText().toString().equals("")) {//�ж�������Ƿ�Ϊ�գ������Ϊ�վͽ���
						IP = Ed_IP.getText().toString();
						port = Integer.parseInt(Ed_post.getText().toString());//��StringתΪint
			    	 }

		    	 toSendCmd("close");
		     }
		});
		
		btnDegree.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (timeON) {             //��timeON = false ʱ����gThread�߳���Ϊֹͣ
					if (gThread != null) {        
						gThread.setRunflag(false);//���gThread��Ϊ�գ��Ͱ�gthread�����߳�״̬�ı�־λ��Ϊfalse����Ϊfalseʱ���߳�ֹͣ
						tVaue.setText("");
						hVaue.setText("");
					}
				} else { //��timeON = true ʱ , ����toGetDegree()����
					if (!Ed_IP.getText().toString().equals("")
							&& !Ed_post.getText().toString().equals("")) {//�ж�������Ƿ�Ϊ�գ������Ϊ�վͽ���
						IP = Ed_IP.getText().toString();
						port = Integer.parseInt(Ed_post.getText().toString());//��StringתΪint
						toGetDegree();//��ʼ�����ݻ�ȡ���̡߳�
					} else {
						Toast.makeText(
								MainActivity.this,
								getResources().getString(
										R.string.txt_IP_or_post_null),
								Toast.LENGTH_LONG).show();   //R.string.txt_IP_or_post_null Ϊres/values/String.xml ���涨��;
					}

				}
			}
		});

	}
	
	private void  toSendCmd(String  cmd){
		gSendCmdThread = new SendCmdThread(cmd);
		Thread thSend = new Thread(gSendCmdThread, "SendCmdThread");
		thSend.start();
	}

	private void toGetDegree() {//��ʼ�����ݻ�ȡ���̡߳�
		gThread = new GetDegreeThread(handler);
		Thread th = new Thread(gThread, "ThreadName");
		th.start();
		timeON = true;
		btnDegree.setText(getResources().getString(R.string.txt_stopdegree));
	}

	/*
	 * Hander ������ɾ�̬�� ��ֹ���ܳ��ֵ��ڴ�й¶
	 */

	static class MsgHandler extends Handler {
		WeakReference<MainActivity> mActivity;

		MsgHandler(MainActivity activity) {
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity _activity = mActivity.get();
			switch (msg.what) {
			case 0:
				_activity.showValue((String) msg.obj);//��ʾ����
				break;
			case 1:
				_activity.setButton(_activity.getResources().getString(
						R.string.txt_getdegree));//��ť�����ָı�

				break;
			case 2:
				Toast.makeText(
						_activity,
						_activity.getResources().getString(
								R.string.txt_connect_timeouot),
						Toast.LENGTH_LONG).show();//��ť�����ָı�
			}
		}
	}

	protected void showValue(String str) {
		
		String[] a = str.split(","); //���ַ��� ����Ϊ��־��ȡÿһ�ε����ݷŵ�String��������
		if(a.length>1){
		tVaue.setText(a[0]);
		hVaue.setText(a[1]);
		}
		
	}

	protected void setButton(String str) {

		 btnDegree.setText( str );

	}
	
	class SendCmdThread implements Runnable {
		private String _cmd;
		private boolean runFlag;

		public SendCmdThread(String cmd) {
			_cmd = cmd;
			runFlag = true;
		}

		public void setRunflag(boolean flag) {
			runFlag = flag;
		}
		
		public void run() {
			Socket bSocket = null;
			OutputStream  out = null;
			try {				
				InetSocketAddress isa = new InetSocketAddress(IP, port);//Ӳ�����ӵĵ�ַ��˿ں�
				bSocket = new Socket();
				bSocket.connect(isa, 3500); // �������ӳ�ʱ�쳣 ..  ��socket����  
				bSocket.setSoTimeout(5000); // ���ö�ȡ��ʱʱ��
					
			} catch (SocketTimeoutException e) {
			
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			while (runFlag) {
				synchronized (this) {
					
					try {
						out = bSocket.getOutputStream();  
						String socketData = _cmd;  
				        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(  
				                  bSocket.getOutputStream()));  
				        writer.write(socketData);  
				        writer.flush();  
				        runFlag=false;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			try {
				if (bSocket != null)
					bSocket.close();
				if (out != null)
					out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	class GetDegreeThread implements Runnable {
		private MsgHandler msgHandler;
		private boolean runFlag;

		public GetDegreeThread(MsgHandler _handler) {
			msgHandler = _handler;
			runFlag = true;
		}

		public void setRunflag(boolean flag) {
			runFlag = flag;
		}

		public void run() {
			Socket aSocket = null;
			InputStream in = null;
			try {
				InetSocketAddress isa = new InetSocketAddress(IP, port);//Ӳ�����ӵĵ�ַ��˿ں�
				aSocket = new Socket();                     
				aSocket.connect(isa, 3500); // �������ӳ�ʱ�쳣 ..  ��socket����  
				aSocket.setSoTimeout(5000); // ���ö�ȡ��ʱʱ��
				in = aSocket.getInputStream(); //��ȡ�����

			} catch (SocketTimeoutException e) {
				msgHandler.sendEmptyMessage(2);
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

			while (runFlag) {
				synchronized (this) {

					byte[] buf = new byte[16];
					int byteCnt = 0;
					try {

						byteCnt = in.read(buf);//�����������ݴ洢��buf���棬byteCntΪ��ȡ�ĳ���

					} catch (Exception e) {
						e.printStackTrace();
					}

					if (byteCnt > 0) { //�����ȡ�ĳ��ȴ���0���ͽ���ȡ�����ݷ���handler����ȥ����
						Message msg = new Message();
						msg.what = 0;
 
						
						
						try {
							msg.obj = new String(buf, 0, byteCnt, "UTF-8");//��buf��UTF-8�ı���תΪ�ַ�����
                            
							Log.i(MY_TAG + "1", "msg.obj: " + (String) msg.obj);
							//Log.i(MY_TAG+"2", "DEGREE: " + new String(buf, 0, byteCnt, "UTF-8"));
						  
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							// Log.i( MY_TAG, "DEGREE: Exception" );
						}
						msgHandler.sendMessage(msg);
					}

				}

//				try {
//					Thread.sleep(500);
//				} catch (Exception ex) {
//
//				}

			} // WHILE

			try {
				if (aSocket != null)
					aSocket.close();
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			timeON = false;

			Message msg = new Message();
			msg.what = 1;
			msgHandler.sendMessage(msg);
		}
	}
	
	protected void onDestroy()
	{
		super.onDestroy();
		if (timeON) {             
			if (gThread != null) {        
				gThread.setRunflag(false);
				tVaue.setText("");
				hVaue.setText("");
			}
		}
		
		if(gSendCmdThread!=null)
			gSendCmdThread.setRunflag(false);
	}

}
