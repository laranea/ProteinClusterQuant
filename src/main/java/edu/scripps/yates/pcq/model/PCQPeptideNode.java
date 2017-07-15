package edu.scripps.yates.pcq.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.analysis.QuantCondition;
import edu.scripps.yates.census.read.model.CensusRatio;
import edu.scripps.yates.census.read.model.IsobaricQuantifiedPeptide;
import edu.scripps.yates.census.read.model.interfaces.QuantRatio;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPSMInterface;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedProteinInterface;
import edu.scripps.yates.census.read.util.QuantUtils;
import edu.scripps.yates.pcq.util.PCQUtils;
import edu.scripps.yates.utilities.model.enums.AggregationLevel;
import edu.scripps.yates.utilities.sequence.PositionInPeptide;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

/**
 * Wrapper class for a peptide node, that could contain more than one
 * {@link QuantifiedPSMInterface} and even different
 * {@link QuantifiedPeptideInterface}.<br>
 * It will contain a {@link QuantRatio} and a confidenceValue associated.
 *
 * @author Salva
 *
 */
public class PCQPeptideNode extends AbstractNode<QuantifiedPeptideInterface> {
	private final static Logger log = Logger.getLogger(PCQPeptideNode.class);

	public static final String INTEGRATED_PEPTIDE_NODE_RATIO = "Integrated peptide node ratio";

	private final Set<QuantifiedPeptideInterface> peptideSet = new THashSet<QuantifiedPeptideInterface>();
	private Double confidenceValue;
	private final Set<QuantRatio> consensusRatios = new THashSet<QuantRatio>();

	private final Set<PCQProteinNode> proteinNodes = new THashSet<PCQProteinNode>();

	private final Map<String, QuantRatio> consensusRatiosByReplicateName = new THashMap<String, QuantRatio>();

	private final ProteinCluster proteinCluster;

	private String key;

	private final Map<QuantifiedPeptideInterface, PositionInPeptide> positionInPeptideByPeptide = new THashMap<QuantifiedPeptideInterface, PositionInPeptide>();

	public PCQPeptideNode(ProteinCluster proteinCluster, Collection<QuantifiedPeptideInterface> peptideCollection) {
		peptideSet.addAll(peptideCollection);
		this.proteinCluster = proteinCluster;
	}

	public PCQPeptideNode(ProteinCluster proteinCluster, String key,
			Pair<QuantifiedPeptideInterface, PositionInPeptide>... peptidesAndPositionInPeptides) {
		for (Pair<QuantifiedPeptideInterface, PositionInPeptide> pair : peptidesAndPositionInPeptides) {
			final QuantifiedPeptideInterface peptide = pair.getFirstelement();
			peptideSet.add(peptide);
			final PositionInPeptide positionInPeptide = pair.getSecondElement();
			positionInPeptideByPeptide.put(peptide, positionInPeptide);
		}
		this.proteinCluster = proteinCluster;
		this.key = key;
	}

	public PCQPeptideNode(ProteinCluster proteinCluster, QuantifiedPeptideInterface... peptides) {
		for (QuantifiedPeptideInterface peptide : peptides) {
			peptideSet.add(peptide);
		}
		this.proteinCluster = proteinCluster;

	}

	public Set<PCQProteinNode> getProteinNodes() {
		return proteinNodes;
	}

	/**
	 * @return the confidenceValue
	 */
	public Double getConfidenceValue() {
		return confidenceValue;
	}

	/**
	 * @param confidenceValue
	 *            the confidenceValue to set
	 */
	public void setConfidenceValue(double confidenceValue) {
		this.confidenceValue = confidenceValue;
	}

	public String getSequence() {
		return PCQUtils.getPeptidesSequenceString(peptideSet);
	}

	public String getFullSequence() {
		return QuantUtils.getPeptidesFullSequenceString(peptideSet);
	}

	public Set<String> getTaxonomies() {
		Set<String> set = new THashSet<String>();
		final Set<QuantifiedProteinInterface> quantifiedProteins = getQuantifiedProteins();
		for (QuantifiedProteinInterface protein : quantifiedProteins) {
			final Set<String> taxonomies = protein.getTaxonomies();
			set.addAll(taxonomies);
		}
		return set;
	}

	@Override
	public String getKey() {
		if (key == null) {
			key = getFullSequence();
		}
		return key;
	}

	public Set<QuantRatio> getRatios() {
		Set<QuantRatio> ratios = new THashSet<QuantRatio>();
		for (QuantifiedPeptideInterface peptide : peptideSet) {
			ratios.addAll(peptide.getRatios());
			for (QuantifiedPSMInterface psm : peptide.getQuantifiedPSMs()) {
				ratios.addAll(psm.getRatios());
			}
		}
		return ratios;
	}

	public QuantRatio getConsensusRatio(QuantCondition cond1, QuantCondition cond2) {
		// first look if there is an externally set consensus ratio that can be
		// come from the ratio integration analysis
		for (QuantRatio ratio : consensusRatios) {
			if (ratio.getCondition1().equals(cond1) && ratio.getCondition2().equals(cond2)) {
				return ratio;
			}
			if (ratio.getCondition1().equals(cond2) && ratio.getCondition2().equals(cond1)) {
				return ratio;
			}
		}
		// return Nan ratio
		CensusRatio ratio = new CensusRatio(Double.NaN, false, cond1, cond2, AggregationLevel.PEPTIDE_NODE,
				INTEGRATED_PEPTIDE_NODE_RATIO);
		return ratio;

	}

	public boolean addConsensusRatio(QuantRatio ratio) {
		return consensusRatios.add(ratio);
	}

	public boolean addConsensusRatio(QuantRatio ratio, String replicateName) {
		if (replicateName != null) {

			return consensusRatiosByReplicateName.put(replicateName, ratio) != null;
		} else {
			return addConsensusRatio(ratio);
		}
	}

	public Set<QuantRatio> getNonInfinityRatios() {
		Set<QuantRatio> ratios = new THashSet<QuantRatio>();
		for (QuantifiedPeptideInterface peptide : peptideSet) {
			ratios.addAll(peptide.getNonInfinityRatios());
			for (QuantifiedPSMInterface psm : peptide.getQuantifiedPSMs()) {
				ratios.addAll(psm.getNonInfinityRatios());
			}
		}
		return ratios;
	}

	@Override
	public Set<QuantifiedProteinInterface> getQuantifiedProteins() {
		Set<QuantifiedProteinInterface> proteins = new THashSet<QuantifiedProteinInterface>();

		for (PCQProteinNode proteinNode : getProteinNodes()) {
			proteins.addAll(proteinNode.getQuantifiedProteins());
		}
		return proteins;
	}

	@Override
	public Set<QuantifiedPSMInterface> getQuantifiedPSMs() {
		Set<QuantifiedPSMInterface> ret = new THashSet<QuantifiedPSMInterface>();
		for (QuantifiedPeptideInterface peptide : getQuantifiedPeptides()) {
			ret.addAll(peptide.getQuantifiedPSMs());
		}
		return ret;
	}

	@Override
	public Set<QuantifiedPeptideInterface> getQuantifiedPeptides() {
		return peptideSet;
	}

	public boolean addQuantifiedPeptide(QuantifiedPeptideInterface peptide) {
		return peptideSet.add(peptide);
	}

	public boolean addQuantifiedPeptide(QuantifiedPeptideInterface peptide, PositionInPeptide positionInPeptide) {
		positionInPeptideByPeptide.put(peptide, positionInPeptide);

		return peptideSet.add(peptide);

	}

	public void removePeptidesFromProteinsInNode() {
		final Iterator<QuantifiedPeptideInterface> peptideSetIterator = peptideSet.iterator();
		while (peptideSetIterator.hasNext()) {
			QuantifiedPeptideInterface peptide = peptideSetIterator.next();
			QuantUtils.discardPeptide(peptide);
			if (peptide.getQuantifiedProteins().isEmpty() || peptide.getQuantifiedPSMs().isEmpty()) {
				peptideSetIterator.remove();
			}
		}
	}

	@Override
	public String toString() {

		return "{" + getKey() + "'}";
	}

	public boolean addProteinNode(PCQProteinNode proteinNode) {
		final boolean added = proteinNodes.add(proteinNode);
		return added;
	}

	public Set<QuantifiedPeptideInterface> getQuantifiedPeptidesInReplicate(String replicateName) {
		Set<QuantifiedPeptideInterface> ret = new THashSet<QuantifiedPeptideInterface>();
		final Set<QuantifiedPeptideInterface> quantifiedPeptides = getQuantifiedPeptides();
		for (QuantifiedPeptideInterface quantifiedPeptideInterface : quantifiedPeptides) {
			if (quantifiedPeptideInterface.getFileNames().contains(replicateName)) {
				ret.add(quantifiedPeptideInterface);
			}
		}
		return ret;
	}

	public QuantRatio getConsensusRatio(QuantCondition quantConditionNumerator,
			QuantCondition quantConditionDenominator, String replicateName) {
		if (replicateName != null) {
			return consensusRatiosByReplicateName.get(replicateName);
		} else {
			return getConsensusRatio(quantConditionNumerator, quantConditionDenominator);
		}
	}

	@Override
	public Set<QuantifiedPeptideInterface> getItemsInNode() {
		return peptideSet;
	}

	public ProteinCluster getCluster() {
		return proteinCluster;
	}

	public Set<QuantifiedProteinInterface> getNonDiscardedQuantifiedProteins() {
		Set<QuantifiedProteinInterface> ret = new THashSet<QuantifiedProteinInterface>();
		for (QuantifiedProteinInterface protein : getQuantifiedProteins()) {
			if (!protein.isDiscarded()) {
				ret.add(protein);
			}
		}
		return ret;
	}

	public boolean containsPTMs() {
		for (QuantifiedPeptideInterface peptide : getItemsInNode()) {
			if (peptide.containsPTMs()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * For a given peptide, gets the {@link PositionInPeptide} for which this
	 * node was created.<br>
	 * Note that this only is used for experiments in which isCollapsedBySites()
	 * is TRUE.
	 * 
	 * @param peptide
	 * @return
	 */
	public PositionInPeptide getPositionInPeptide(QuantifiedPeptideInterface peptide) {
		if (!this.peptideSet.contains(peptide)) {
			throw new IllegalArgumentException(
					"The peptide " + peptide.getSequence() + " (" + peptide.getKey() + ") is not in this peptide node");
		}
		if (positionInPeptideByPeptide.containsKey(peptide)) {
			return this.positionInPeptideByPeptide.get(peptide);
		} else {
			return null;
		}
	}

	public List<Pair<IsobaricQuantifiedPeptide, PositionInPeptide>> getPeptidesWithPositionsInPeptide() {
		List<Pair<IsobaricQuantifiedPeptide, PositionInPeptide>> isobaricPeptidesAndPositionsInPeptides = new ArrayList<Pair<IsobaricQuantifiedPeptide, PositionInPeptide>>();
		final Set<QuantifiedPeptideInterface> peptides = getQuantifiedPeptides();
		for (QuantifiedPeptideInterface peptide : peptides) {
			if (peptide instanceof IsobaricQuantifiedPeptide) {
				Pair<IsobaricQuantifiedPeptide, PositionInPeptide> pair = new Pair<IsobaricQuantifiedPeptide, PositionInPeptide>(
						(IsobaricQuantifiedPeptide) peptide, getPositionInPeptide(peptide));
				isobaricPeptidesAndPositionsInPeptides.add(pair);
			}
		}
		Collections.sort(isobaricPeptidesAndPositionsInPeptides,
				new java.util.Comparator<Pair<IsobaricQuantifiedPeptide, PositionInPeptide>>() {

					@Override
					public int compare(Pair<IsobaricQuantifiedPeptide, PositionInPeptide> arg0,
							Pair<IsobaricQuantifiedPeptide, PositionInPeptide> arg1) {
						return arg0.getFirstelement().getFullSequence()
								.compareTo(arg1.getFirstelement().getFullSequence());
					}
				});
		return isobaricPeptidesAndPositionsInPeptides;
	}

}
