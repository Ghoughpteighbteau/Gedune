package data.scripts.world;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

public class GeduneStationManager extends BaseSpawnPoint {
	private static final float FUEL_LIMIT = 1000.0f;
	private static final float SUPL_LIMIT = 800.0f; 
	private static final float FUEL_GEN = 5f; 
	private static boolean NAG = false; //debug messages
	
	//bonus Fuel is a balancing force. Meant to simulate the gedune
	//setting up temporary reactors in response to resource surplus. 
	private float bonusFuel = 0;
	
	private SectorEntityToken refactory;
	private CargoAPI refCargo;

	public GeduneStationManager(SectorAPI sector, LocationAPI location, float daysInterval
			        , SectorEntityToken anchor) {
		super(sector, location, daysInterval, 1, anchor);
		refactory = anchor;
		refCargo = refactory.getCargo();
	}

	@Override
	protected CampaignFleetAPI spawnFleet() {

		// bonus fuel is added gradually, 30 every turn
		refCargo.addFuel(FUEL_GEN + (bonusFuel<30 ? bonusFuel : 30));
		if(bonusFuel < 30) bonusFuel = 0;
		else			   bonusFuel -= 30;
		
		// if we have too little fuel and too much supplies, break supplies down for fuel
		if (refCargo.getFuel() < FUEL_LIMIT && refCargo.getSupplies() > SUPL_LIMIT){
			bonusFuel += 300;
			refCargo.removeSupplies(600);
			
		//Break an unwanted weapon down for supplies
		}else if (refCargo.getSupplies() < SUPL_LIMIT || Math.random() < 0.3)
			breakDown();
		
		//Build an wanted weapon with supplies
		else if (Math.random() < 0.5)
			buildWep();
		
		//Build either a ship or a fighter wing.
		else if (Math.random() < (float)makeShip.length/(makeShip.length+makeWing.length))
			shipMake(FleetMemberType.SHIP, makeShip[(int)(Math.random()*makeShip.length)]);
		else 
			shipMake(FleetMemberType.FIGHTER_WING, makeWing[(int)(Math.random()*makeWing.length)]);

		return null;
	}

	private Boolean isListed(String ID, String[] list){
		for(int i = 0; i < list.length; i++)
			if(ID.equals(list[i]))
				return true;
		return false;
	}
	
	private class ItemComp implements Comparator{
		public int compare(Object item1, Object item2){
			int item1num = (int)((CargoStackAPI)item1).getSize();
			int item2num = (int)((CargoStackAPI)item2).getSize();
			
			if(item1num > item2num)
				return 1;
			else if(item1num < item2num)
				return -1;
			else 
				return 0;			
		}
	}
	
	private void breakDown() {
		FleetDataAPI dockyard = refCargo.getMothballedShips();
		List moths = dockyard.getMembersListCopy();
		FleetMemberAPI breakShip = null;
		
		for (int i = 0; i < moths.size(); i++) {
			breakShip = (FleetMemberAPI)moths.get(i);
			if(! isListed(breakShip.getSpecId(),makeShip) && 
				! isListed(breakShip.getSpecId(),makeWing)){
				if(Global.getSettings().getBoolean("devMode") && NAG) 
					Global.getSector().addMessage("Eating Ship: " + breakShip.getSpecId(), Color.YELLOW);
				refCargo.addSupplies(shipSupplyCost(breakShip) * 0.9f);
				refCargo.removeFuel(breakShip.getFleetPointCost() * 10);
				dockyard.removeFleetMember(breakShip);
				return;
			}
		}
		
		List wepList = refCargo.getStacksCopy();
		Collections.sort(wepList, new ItemComp());

		String breakWep;
		for (int i = 0; i < wepList.size(); i++ ){
			if (!((CargoStackAPI)wepList.get(i)).isWeaponStack()) continue;

			breakWep = (String) ((CargoStackAPI)wepList.get(i)).getData();
			if (! isListed(breakWep,makeWep)){
				if(Global.getSettings().getBoolean("devMode") && NAG) 
					Global.getSector().addMessage("Eating Weapon: " + breakWep, Color.YELLOW);
				refCargo.addSupplies(40);
				refCargo.removeFuel(25);
				refCargo.removeWeapons(breakWep, 1);
				return;
			}
		}
	}

	private void buildWep() {
		refCargo.removeFuel(50f);
		refCargo.removeSupplies(50f);
		refCargo.addWeapons(makeWep[(int)(Math.random()*makeWep.length)], 1);
	}

	private void shipMake(FleetMemberType wingOrShip, String ID) {
		FleetMemberAPI ship = Global.getFactory().createFleetMember(wingOrShip, ID);
		float fuelCost = ship.getFleetPointCost() * 15;
		float supCost = shipSupplyCost(ship);
		if(fuelCost < refCargo.getFuel() && supCost < refCargo.getSupplies()){
			if(Global.getSettings().getBoolean("devMode") && NAG) 
				Global.getSector().addMessage("Building Ship: " + ID, Color.YELLOW);
			refCargo.removeFuel(fuelCost);
			refCargo.removeSupplies(supCost);
			refCargo.getMothballedShips().addFleetMember(ship);
		} else breakDown(); // grr. Need More. 
	}

	private float shipSupplyCost(FleetMemberAPI ship) {
		float buf = 0;
		if (ship.isFighterWing())	buf = 8;
		else if (ship.isFrigate())	buf = 16;
		else if (ship.isDestroyer())buf = 35;
		else if (ship.isCruiser())	buf = 90;
		else 								buf = 145;

		buf *= ship.getFleetPointCost();
		return buf;
	}
	
	private static String[] makeWep = { "gedune_plasma", "gedune_repeater", "gedune_flarepd",
			"annihilator", "lightag", "taclaser", "irpulse", "gedune_maelstrom", "gedune_scythe",
			"phasebeam", "hurricane", "gedune_hipdlaser"};
	private static String[] makeShip = { "gedune_byakuri_Hull",
			"gedune_kitsune_Hull", "gedune_mozok_Hull", "gedune_kyirus_Hull", "gedune_mozok_f_Hull", "gedune_mozok_t_Hull",
			"gedune_nanda_Hull", "gedune_tenzen_Hull", "gedune_bakoros_Hull" };
	private static String[] makeWing = { "gedune_chua_wing", "gedune_duri_wing" };
}