import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.IOException;

public class CookieClient{

	public static void main(String[] args){

		try{
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			System.out.println("Connecting to " + host + ":" + port + "...");

			Socket client = new Socket(host, port);
			System.out.println("Connection established");

			InputStream clientStream = client.getInputStream();
			byte[] readBuffer = new byte[50];
			int numRead = clientStream.read(readBuffer);
			String fortune = new String(readBuffer);
			System.out.println("Your fortune: " + fortune);

			clientStream.close();
			client.close();
			System.out.println("Exiting");
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
}