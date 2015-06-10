import java.io.ByteArrayOutputStream;


public class TestMe {
	
	public static void manipulateArray(byte[] array) {
		array[0] = 5;
		array[1] = 6;
		array[2] = 7;
		array[3] = 8;
	}
	
	public static <?> void getList() {
	}
	
	public static void main(String args[]) {
/*		byte[] bites = new byte[4];
		bites[0] = 1;
		bites[1] = 2;
		bites[2] = 3;
		bites[3] = 4;
		
		manipulateArray(bites);
		for(byte b : bites) {
			System.out.println(b);
		}*/
		
/*		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		GenerateNumbers gn = new GenerateNumbers(stream);
		ConsumeNumbers cn = new ConsumeNumbers(stream);
		gn.start();
		cn.start();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		gn.stopIt();
		cn.stopIt();*/
	}
}

class GenerateNumbers extends Thread {
	boolean stopped = false;
	ByteArrayOutputStream stream;
	GenerateNumbers(ByteArrayOutputStream baos) {
		stream = baos;
	}
	
	@Override
	public void run() {
		int i = 0;
		while(!stopped) {
			stream.write(i);
			System.out.print("G:" + i);
			i++;
		}
	}	
	
	public void stopIt() {
		stopped = true;
	}
}

class ConsumeNumbers extends Thread {
	boolean stopped = false;
	ByteArrayOutputStream stream;
	ConsumeNumbers(ByteArrayOutputStream baos) {
		stream = baos;
	}
	
	@Override
	public void run() {
		while(!stopped) {
			System.out.println("Size:" + stream.size());
			byte[] bites = stream.toByteArray();
			for(int i = 0; i < bites.length; i++) {
				System.out.print("C:" + (int)bites[i]);
			}
		}
	}	
	
	public void stopIt() {
		stopped = true;
	}
}
