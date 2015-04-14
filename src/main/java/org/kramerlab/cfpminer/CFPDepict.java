package org.kramerlab.cfpminer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.mg.javalib.gui.MultiImageIcon;
import org.mg.javalib.gui.MultiImageIcon.Layout;
import org.mg.javalib.gui.MultiImageIcon.Orientation;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ColorUtil;
import org.mg.javalib.util.ImageLoader;
import org.mg.javalib.util.SwingUtil;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.HighlightGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class CFPDepict
{

	public static void drawToPNG(String pngFile, IAtomContainer mol, int size) throws Exception
	{
		BufferedImage image = draw(mol, size);
		ImageIO.write((RenderedImage) image, "PNG", new File(pngFile));
	}

	public static BufferedImage draw(IAtomContainer mol, int size) throws Exception
	{
		IAtomContainerSet set = ConnectivityChecker.partitionIntoMolecules(mol);
		BufferedImage image;
		if (set.getAtomContainerCount() < 2)
			image = drawConnected(mol, size);
		else
		{
			List<ImageIcon> icons = new ArrayList<ImageIcon>();
			for (int i = 0; i < set.getAtomContainerCount(); i++)
				icons.add(new ImageIcon(drawConnected(set.getAtomContainer(i), -1)));
			image = (BufferedImage) new MultiImageIcon(icons, Layout.horizontal, Orientation.center, 2).getImage();
			if (size != -1)
				image = (BufferedImage) ImageLoader.getShrinkedImage(new ImageIcon(image), size, size).getImage();
		}
		return image;
	}

	@SuppressWarnings("unchecked")
	public static BufferedImage drawConnected(IAtomContainer mol, int size) throws CDKException
	{

		//		IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles("NCC");
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		sdg.setMolecule(mol);
		sdg.generateCoordinates();
		mol = sdg.getMolecule();

		@SuppressWarnings("rawtypes")
		List generators = new ArrayList<>();
		generators.add(new BasicSceneGenerator());
		generators.add(new BasicBondGenerator());
		generators.add(new BasicAtomGenerator());
		AtomContainerRenderer renderer = new AtomContainerRenderer(generators, new AWTFontManager());

		int width;
		int height;
		if (size == -1)
		{
			renderer.setup(mol, new Rectangle(0, 0, 1, 1));
			Rectangle diagramRectangle = renderer.paint(mol, new AWTDrawVisitor((Graphics2D) new BufferedImage(1, 1,
					BufferedImage.TYPE_INT_ARGB).getGraphics()));
			width = (int) (1.5 * (diagramRectangle.getWidth() + diagramRectangle.x));
			height = (int) (1.5 * (diagramRectangle.getHeight() + diagramRectangle.y));
			width = Math.max(20, width);
			height = Math.max(20, height);
		}
		else
		{
			width = size;
			height = size;
		}

		Rectangle drawArea = new Rectangle(width, height);
		renderer.setup(mol, drawArea);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		//		g2.setColor(Color.WHITE);
		//		g2.fillRect(0, 0, width, height);
		renderer.paint(mol, new AWTDrawVisitor(g2), drawArea, true);

		return image;
	}

	public static void drawFPtoPng(String pngFile, IAtomContainer mol, int atoms[], boolean crop, int size)
			throws Exception
	{
		BufferedImage image = drawFP(mol, atoms, crop, size);
		ImageIO.write((RenderedImage) image, "PNG", new File(pngFile));
	}

	public static BufferedImage drawFP(IAtomContainer mol, int atoms[], boolean crop, int size) throws Exception
	{
		IAtomContainerSet set = ConnectivityChecker.partitionIntoMolecules(mol);
		BufferedImage image = null;
		if (set.getAtomContainerCount() < 2)
			image = drawFPConnected(mol, atoms, crop, size);
		else
		{
			int minAtomIdx = ArrayUtil.getMinMax(atoms)[0];
			List<ImageIcon> icons = new ArrayList<ImageIcon>();
			atoms = Arrays.copyOf(atoms, atoms.length);
			for (int i = 0; i < set.getAtomContainerCount(); i++)
			{
				if (minAtomIdx < 0 || minAtomIdx >= set.getAtomContainer(i).getAtomCount())
				{
					if (!crop)
						icons.add(new ImageIcon(draw(set.getAtomContainer(i), -1)));
				}
				else
				{
					image = drawFPConnected(set.getAtomContainer(i), atoms, crop, crop ? size : -1);
					icons.add(new ImageIcon(image));
				}
				minAtomIdx -= set.getAtomContainer(i).getAtomCount();
				for (int j = 0; j < atoms.length; j++)
					atoms[j] -= set.getAtomContainer(i).getAtomCount();
			}
			if (!crop)
			{
				image = (BufferedImage) new MultiImageIcon(icons, Layout.horizontal, Orientation.center, 2).getImage();
				if (size != -1)
					image = (BufferedImage) ImageLoader.getShrinkedImage(new ImageIcon(image), size, size).getImage();
			}
		}
		return image;
	}

	@SuppressWarnings("unchecked")
	public static BufferedImage drawFPConnected(IAtomContainer mol, int atoms[], boolean crop, int size)
			throws Exception
	{
		if (atoms == null || atoms.length == 0)
			if (crop)
				throw new IllegalArgumentException();
		if (crop && size == -1)
			throw new IllegalArgumentException();

		//		IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles("NCC");
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		sdg.setMolecule(mol);
		sdg.generateCoordinates();
		mol = sdg.getMolecule();
		HashMap<IChemObject, Integer> ids = new HashMap<IChemObject, Integer>();
		IAtomContainer s = new AtomContainer();
		for (int j = 0; j < mol.getAtomCount(); j++)
			if (atoms != null && ArrayUtil.indexOf(atoms, j) != -1)
			{
				ids.put(mol.getAtom(j), 1);
				//				System.out.println("s " + mol.getAtom(j).getPoint2d());
				s.addAtom(mol.getAtom(j));
			}
		//			else
		//				System.out.println("n " + mol.getAtom(j).getPoint2d());
		mol.setProperty(HighlightGenerator.ID_MAP, ids);

		@SuppressWarnings("rawtypes")
		List generators = new ArrayList<>();
		generators.add(new BasicSceneGenerator());
		generators.add(new HighlightGenerator());
		generators.add(new BasicBondGenerator());
		generators.add(new BasicAtomGenerator());
		AtomContainerRenderer renderer = new AtomContainerRenderer(generators, new AWTFontManager());

		renderer.getRenderer2DModel().set(HighlightGenerator.HighlightPalette.class,
				HighlightGenerator.createPalette(null, ColorUtil.transparent(Color.RED, 150)));
		renderer.getRenderer2DModel().set(HighlightGenerator.HighlightRadius.class, 15.0);

		// determine preferred size of molecule
		renderer.setup(mol, new Rectangle(0, 0, 1, 1));
		Rectangle diagramRectangle = renderer.paint(mol, new AWTDrawVisitor((Graphics2D) new BufferedImage(1, 1,
				BufferedImage.TYPE_INT_ARGB).getGraphics()));
		int width = (int) (1.5 * (diagramRectangle.getWidth() + diagramRectangle.x));
		int height = (int) (1.5 * (diagramRectangle.getHeight() + diagramRectangle.y));
		width = Math.max(20, width);
		height = Math.max(20, height);

		// draw according to preferred size (with 10 pixels extra for the highlights)
		Rectangle drawArea = new Rectangle(5, 5, width - 10, height - 10);
		renderer.setup(mol, drawArea);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height);
		renderer.paint(mol, new AWTDrawVisitor(g2), drawArea, true);

		final Rectangle2D r = renderer.calculateDiagramBounds(s);
		int x, y, w, h;
		if (size >= width)
		{
			x = 0;
			w = width - 1;
		}
		else
		{
			x = (int) (r.getX() - (size - r.getWidth()) / 2.0);
			if (x + size > width)
				x -= (x + size - width);
			if (x < 0)
				x = 0;
			w = size;
		}
		if (size >= height)
		{
			y = 0;
			h = height - 1;
		}
		else
		{
			y = (int) (r.getY() - (size - r.getHeight()) / 2.0);
			if (y + size > height)
				y -= (y + size - height);
			if (y < 0)
				y = 0;
			h = size;
		}
		final Rectangle2D r2 = r.getFrame();
		r2.setFrame(x, y, w, h);
		//		System.out.println(r);
		//		System.out.println(r2);
		//		System.out.println(image.getWidth() + ", " + image.getHeight());

		//		JLabel l = new JLabel(new ImageIcon(image))
		//		{
		//			@Override
		//			public void paint(Graphics g)
		//			{
		//				super.paint(g);
		//				g.setColor(Color.GREEN);
		//				g.drawRect(0, 0, width, height);
		//				g.setColor(Color.RED);
		//				g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
		//				g.setColor(Color.BLUE);
		//				g.drawRect((int) r2.getX(), (int) r2.getY(), (int) r2.getWidth(), (int) r2.getHeight());
		//
		//			}
		//		};
		//		SwingUtil.showInDialog(l);

		if (crop)
			image = image.getSubimage(x, y, w, h);
		if (size != -1 && (image.getWidth() > size || image.getHeight() > size))
			image = (BufferedImage) ImageLoader.getShrinkedImage(new ImageIcon(image), size, size).getImage();
		if (crop && (image.getWidth() < size || image.getHeight() < size))
		{
			BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics g = img.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, size, size);
			g.drawImage(image, (size - image.getWidth()) / 2, (size - image.getHeight()) / 2, null);
			image = img;
		}
		return image;
	}

	private static JLabel getLabel(BufferedImage img, String text)
	{
		BufferedImage imgW = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		imgW.getGraphics().setColor(Color.WHITE);
		imgW.getGraphics().fillRect(0, 0, imgW.getWidth(), imgW.getHeight());
		imgW.getGraphics().drawImage(img, 0, 0, null);
		JLabel l = new JLabel(text, new ImageIcon(imgW), SwingConstants.CENTER);
		l.setHorizontalTextPosition(JLabel.CENTER);
		l.setVerticalTextPosition(JLabel.BOTTOM);
		return l;
	}

	public static void demo() throws InvalidSmilesException, Exception
	{
		{
			DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p"));
			for (Integer size : new Integer[] { 100, -1 })
			{
				for (String smiles : new String[] { "[Na+].[Na+].O=S(C1=CC=C(C(C)CC)C=C1)([O-])=O",
						"CC(C)C(C1=CC=C(C=C1)Cl)C(=O)OC(C#N)C2=CC(=CC=C2)OC3=CC=CC=C3" })
				{
					BufferedImage img = draw(
							new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles), size);
					b.append(getLabel(img, smiles + " size:" + size));
				}
			}
			SwingUtil.showInFrame(b.getPanel());
		}
		{
			DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p"));
			int atoms[] = new int[] { 1, 2, 3 };
			for (Integer size : new Integer[] { 75, -1 })
			{
				for (boolean crop : new boolean[] { true, false })
				{
					if (crop && size == -1)
						continue;

					for (String smiles : new String[] { "O=C1C2=C(C=CC=C2)C(=O)C3=C1C=CC=C3",
							"C1(=C(C=CC=C1)N)OC.[H]Cl" })
					{
						BufferedImage img = drawFP(
								new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles), atoms,
								crop, size);
						b.append(getLabel(img, smiles + " crop:" + crop + " size:" + size));
					}
				}
			}
			SwingUtil.showInFrame(b.getPanel());
		}
		System.exit(1);

	}

	public static void main(String[] args) throws InvalidSmilesException, Exception
	{
		demo();
	}
}
