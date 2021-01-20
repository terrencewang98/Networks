import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.io.IOException;

public class CookieServer{

	public static void main(String[] args){
	
		try{
			int port = Integer.parseInt(args[0]);
			ServerSocket server = new ServerSocket(port);
			System.out.println("Listening on port " + port);

			Socket client = server.accept();
			System.out.println("Connection established");

			OutputStream clientStream = client.getOutputStream();
			String[] fortunes = {"You will see it when you believe it",
								"Embrace a new narrative",
								"There are no rules",
								"Be humble and proud of yourself at the same time",
								"It's your duty to dream"};
			int rng = (int)(Math.random()*5);
			String fortune = fortunes[rng];
			clientStream.write(fortune.getBytes());
			clientStream.close();
			System.out.println("Fortune sent");

			client.close();
			server.close();
			System.out.println("Exiting");
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		
	}
}