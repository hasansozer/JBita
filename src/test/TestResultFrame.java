package test;

import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import java.awt.image.BufferedImage;

public class TestResultFrame extends JDialog {
	
	public TestResultFrame(String umlSeqDiagImgFilename) {
        
		BufferedImage image = readImage(umlSeqDiagImgFilename);
		
		if(image != null) {
			ImageIcon imageIcon = new ImageIcon(image);
			JLabel imageLabel = new JLabel();
			imageLabel.setIcon(imageIcon);
			this.getContentPane().add(new JScrollPane(imageLabel));
			this.pack();
			this.setLocationRelativeTo(null);
			this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			this.setModal(true);
			this.setVisible(true);
		} else
			this.dispose();
	}
	
	private BufferedImage readImage(String umlSeqDiagImgFilename) {
		BufferedImage image = null;
		try
        {
        	image = ImageIO.read(new File(umlSeqDiagImgFilename));
        }
        catch (Exception e)
        {
          System.out.println("Error while reading the image file " + umlSeqDiagImgFilename);
          e.printStackTrace();
          System.exit(1);
        }
		return image;
	}
}
