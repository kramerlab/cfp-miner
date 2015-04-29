package org.kramerlab.cfpminer.cdk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.INChIPlainTextReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

public class CDKUtil
{
	public static List<IChemObject> getAtomsAndBonds(IAtomContainer mol)
	{
		List<IChemObject> l = new ArrayList<>();
		for (int i = 0; i < mol.getAtomCount(); i++)
			l.add(mol.getAtom(i));
		for (int i = 0; i < mol.getBondCount(); i++)
			l.add(mol.getBond(i));
		return l;
	}

	public static String toSmiles(IAtomContainer mol) throws CDKException
	{
		return new SmilesGenerator().create(mol);
	}

	public static String toInchi(IAtomContainer mol) throws CDKException
	{
		return InChIGeneratorFactory.getInstance().getInChIGenerator(mol).getInchi();
	}

	public static String toInchiKey(IAtomContainer mol) throws CDKException
	{
		return InChIGeneratorFactory.getInstance().getInChIGenerator(mol).getInchiKey();
	}

	private static HashMap<String, IAtomContainer> smilesToMol = new HashMap<String, IAtomContainer>();

	public static synchronized IAtomContainer parseSmiles(String smiles) throws InvalidSmilesException
	{
		if (!smilesToMol.containsKey(smiles))
			smilesToMol.put(smiles, new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles));
		return smilesToMol.get(smiles);
	}

	public static void setMolForSmiles(String smi, IAtomContainer a)
	{
		smilesToMol.put(smi, a);
	}

	private static HashMap<String, IAtomContainer> inchiToMol = new HashMap<String, IAtomContainer>();

	public static IAtomContainer parseInchi(String inchi) throws IOException, CDKException
	{
		if (!inchiToMol.containsKey(inchi))
		{
			INChIPlainTextReader reader = new INChIPlainTextReader(new ByteArrayInputStream(inchi.getBytes()));
			IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
			reader.close();
			List<IAtomContainer> l = ChemFileManipulator.getAllAtomContainers(content);
			if (l.size() != 1)
				throw new RuntimeException("Could not read inchi: " + inchi);
			IAtomContainer m = l.get(0);
			inchiToMol.put(inchi, m);
		}
		return inchiToMol.get(inchi);
	}

	public static void validateSmiles(String smiles) throws InvalidSmilesException
	{
		if (parseSmiles(smiles) == null || parseSmiles(smiles).getAtomCount() == 0)
			throw new InvalidSmilesException("not a valid smiles: '" + smiles + "'");
	}
}
