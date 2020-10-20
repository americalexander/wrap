package edu.utexas.wrap.demand.containers;

import java.util.Collection;

import edu.utexas.wrap.TimePeriod;
import edu.utexas.wrap.demand.DemandMap;
import edu.utexas.wrap.demand.ODMatrix;
import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.net.TravelSurveyZone;

public class FixedSizeODMatrix<T extends DemandMap> implements ODMatrix {
	
	private float vot;
	private final Mode mode;
	private final Collection<TravelSurveyZone> zones;
	private final DemandMap[] demandMaps;
	private TimePeriod tp;
	
	public FixedSizeODMatrix(Float vot, Mode mode, Collection<TravelSurveyZone> zones) {
		this.mode = mode;
		this.vot = vot;
		this.zones = zones;
		this.demandMaps = new DemandMap[zones.size()];
	}

	@Override
	public Mode getMode() {
		return mode;
	}

	@Override
	public float getDemand(TravelSurveyZone origin, TravelSurveyZone destination) {
		return 	demandMaps[origin.getOrder()].get(destination);
	}

	@Override
	public void put(TravelSurveyZone origin, TravelSurveyZone destination, Float demand) {
		demandMaps[origin.getOrder()].put(destination, demand);
	}

	@Override
	public Float getVOT() {
		return vot;
	}

	@Override
	public void setVOT(float VOT) {
		this.vot = VOT;
	}

	@Override
	public Collection<TravelSurveyZone> getZones() {
		return zones;
	}

	@Override
	public DemandMap getDemandMap(TravelSurveyZone origin) {
		return demandMaps[origin.getOrder()];
	}
	
	public void setDemandMap(TravelSurveyZone origin, T demandMap) {
		demandMaps[origin.getOrder()] = demandMap;
	}

	
	public String toString() {
		return mode + "_" + getVOT();
	}

	@Override
	public TimePeriod timePeriod() {
		// TODO Auto-generated method stub
		return tp;
	}
}
