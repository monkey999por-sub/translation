package thread;

import client.MyCloudVisionClient;
import com.google.common.io.ByteArrayDataInput;
import common.MyClipBoard;
import setting.Setting;
import worker.TranslationWorker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.*;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;

public class ClipBoardObserver implements Runnable {

	//前回のクリップボードの内容を保持する
	private static String lastTimeClipText = MyClipBoard.getText() == null ? "" : MyClipBoard.getText();

	public ClipBoardObserver() {
	}

	@Override
	public void run() {
		// クリップボードを監視しながらポーリング
		int gcCount = 0;
		Long loopInterval = Long.valueOf(Setting.get("loop_interval"));

		while (true) {
			if (lastTimeClipText.equals(MyClipBoard.getText())) {
				//一定時間経過後にGCする 1800ループごと
				if (gcCount++ > 1800) {
					System.out.println("Garbage Collection called");
					Runtime.getRuntime().gc();
					gcCount = 0;
				}
				try {
					Thread.sleep(loopInterval);
				} catch (InterruptedException e) {
					if (Boolean.valueOf(Setting.get("debug_mode"))) {
						e.printStackTrace();
					}
				}
				continue;
			}

			// translation worker run
			String ct = MyClipBoard.getText();
//			TranslationWorker.run(ct);
			lastTimeClipText = ct;

			try {
				Image image = MyClipBoard.getImage();
				PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, -1, -1, false);
				pixelGrabber.grabPixels();
				ColorModel cm = pixelGrabber.getColorModel();

				final int w = pixelGrabber.getWidth();
				final int h = pixelGrabber.getHeight();
				WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
				BufferedImage renderedImage =
						new BufferedImage(
								cm,
								raster,
								cm.isAlphaPremultiplied(),
								new Hashtable());
//				renderedImage.getRaster().setDataElements(0, 0, w, h, pixelGrabber.getPixels());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(renderedImage, "PNG", baos);
				byte[] bytes = baos.toByteArray();


				MyCloudVisionClient.request(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (UnsupportedFlavorException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
