package Server;



import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;


public class MsgServer{
	 private MulticastSocket groupSocket = null;
	 private boolean tokenIn = false;
	 private int serverIndex;
	 public MsgServer(int index) throws IOException{
		 serverIndex = index;
		 groupSocket = new MulticastSocket(Integer.valueOf(Configure.getInstance().getValue("ServerPort"+index)));
		 //groupSocket.setLoopbackMode(true);
		 InetAddress group = InetAddress.getByName(Configure.getInstance().getValue("ServerGroup"+index));
		 groupSocket.joinGroup(group);
		 tokenIn = true;
		 new MainServer().start();
	 }
	 
	 class MainServer extends Thread{
		 public MainServer(){}
		 public void run(){
			 while (true){
				byte[] recvBuf = new byte[5000];
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				try{
					groupSocket.receive(packet);
					Message msg = PacketHandle.getMessage(packet, recvBuf);
					System.out.println(serverIndex + " Receive: "+msg.getData().toString() + "\n");
					if (msg != null){
						RequestHandle requestHandle = new RequestHandle(msg);
						requestHandle.start();
						stop();
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			 }
		 }
	 }
	 
	 class RequestHandle extends Thread{
		private Message msg;

		public RequestHandle(Message msg) {
			this.msg = msg;
		}

		@SuppressWarnings("unchecked")
		public void run() {
			if (msg.getOperation() == Message.PUT) {
				@SuppressWarnings("rawtypes")
				HashMap data = msg.getData();
				KVSServer.kvs.Put((String) data.get("key"),
						(byte[]) data.get("value"));
				data.put("staatus", "put success");
				msg.setData(data);
				try {
					groupSocket.send(PacketHandle.getDatagram(msg,serverIndex));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (msg.getOperation() == Message.GET) {
				if (tokenIn){
					@SuppressWarnings("rawtypes")
					HashMap data = msg.getData();
					@SuppressWarnings("unused")
					byte[] value = KVSServer.kvs.Get((String) data.get("key"));
					// if success
					Message success = new Message();
					data.put("value", data);
					success.setData(data);
					try {
						groupSocket.send(PacketHandle.getDatagram(success,serverIndex));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

//public class MsgServer extends ServerSocket {
//	private int seq = 0;
//
//	//List<CreateServerThread> waitingThread = new ArrayList<CreateServerThread>();
//	List<CreateServerThread> servingThread = new ArrayList<CreateServerThread>();
//	NewClient new_client;
//	
//    public MsgServer(String addr,int port) throws IOException{
//    	super(port);
//    	new_client = new NewClient();
//    	new_client.start();
//    }
//    class NewClient extends Thread{
//    	public NewClient(){}
//    	public void run(){
//    		try{
//    			try{
//    	    		while(true){
//    	    			Socket socket = accept();
//    	    			CreateServerThread client = new CreateServerThread(socket);
//    	    			servingThread.add(client);
//    	    			client.start();
//    	    		}
//    	    	}catch(IOException e){}
//    	    	finally{
//    	    		close();
//    	    	}
//    		}catch(Exception e){}
//    	}
//    }
//    //each thread for each client
//    class CreateServerThread extends Thread{
//    	Socket client;
//    	InputStream is;
//    	ObjectInputStream dis;
//    	OutputStream outputStream;
//		ObjectOutputStream dos;
//
//    	private final ReentrantLock lock = new ReentrantLock();
//    	String name;
//    	
//    	public CreateServerThread(Socket s) throws IOException{
//    		client = s;
//    		try{
//    			is = s.getInputStream();
//    			dis = new ObjectInputStream(is);
//    			outputStream = s.getOutputStream();
//				dos = new ObjectOutputStream(outputStream);
//    		}catch(Exception e){e.printStackTrace();}
//    	} 	
//    	public void run(){
//    		try{
//    			while(true){
//    				Message msg = (Message)dis.readObject(); // receive from client
//    				if(msg.operation == Message.PUT){
//    					HashMap data = msg.getData();
//    					KVSServer.kvs.Put((String)data.get("key"), (byte[])data.get("value"));
//    				}
//    				if(msg.operation == Message.GET){
//    					HashMap data = msg.getData();
//    					byte[] value = KVSServer.kvs.Get((String)data.get("key"));
//    					// if success
//    					Message success = new Message();
//    					data.put("value", data);
//    					success.setData(data);
//    					SendMsg(success);
//    				}
//    			}
//    		}catch(Exception e){}
//    	}
//    	public void SendMsg(Message msg){ //send to client
//    		try{
//    			dos.writeObject(msg);
//				dos.flush();
//				dos.reset();
//    		}catch(IOException ex){} 
//    	}
//    }
//}