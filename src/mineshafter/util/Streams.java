package mineshafter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
	public static int pipeStreams(InputStream in,OutputStream out) throws IOException {
		byte[] b = new byte[8192];
		int read;
		int total=0;
		while(true) {
			try {
				read = in.read(b);
				if(read == -1) break;
			} catch(IOException e) {
				break;
			}
			out.write(b, 0, read);
			total += read;
		}
		out.flush();
		return total;
	}
	public static void pipeStreamsActive(final InputStream in,final OutputStream out) {
		Thread thread = new Thread("Active Pipe Thread") {
			public void run() {
				byte[] b = new byte[8192];
				int count;
				while(true) {
					try {
					count = in.read(b);
					if(count == -1) return;
					out.write(b, 0, count);
					out.flush();
					} catch(IOException e) {
						return;
					}
				}
			}
		};
		thread.start();
	}
}