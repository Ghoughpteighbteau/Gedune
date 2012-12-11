package data.scripts.world;

import com.fs.starfarer.api.Global;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.*;

import data.scripts.world.*;

public class SectorGenWithGedune implements SectorGeneratorPlugin {
	public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.getStarSystem("Corvus");

		SectorEntityToken star = system.createToken(0, 0);

		SectorEntityToken planet = system.addPlanet(star, "Unidentified",
				"toxic", 270, 75, 9600, 150);
		SectorEntityToken GDstation = system.addOrbitalStation(planet, 45, 300,
				50, "Refactory", "gedune");

		GeduneSpawnPoint GDspawn = new GeduneSpawnPoint(sector, system, 6, 8,
				GDstation);

		system.addSpawnPoint(GDspawn);
		system.addSpawnPoint(new GeduneStationManager(sector, system, 4, GDstation));

		for (int i = 0; i < 5; i++)
			GDspawn.spawnFleet();

		CargoAPI GDcargo = GDstation.getCargo();
		GDcargo.addWeapons("gedune_plasma", (int) (Math.random() * 4) + 1);
		GDcargo.addWeapons("gedune_repeater", (int) (Math.random() * 3) + 3);
		GDcargo.addWeapons("gedune_flarepd", (int) (Math.random() * 3) + 3);
		GDcargo.addWeapons("annihilator", (int) (Math.random() * 5) + 5);
		GDcargo.addWeapons("lightag", (int) (Math.random() * 3) + 5);
		GDcargo.addWeapons("taclaser", (int) (Math.random() * 3) + 5);
		GDcargo.addWeapons("irpulse", (int) (Math.random() * 3) + 5);
		GDcargo.addSupplies((int) (Math.random() * 150) + 150);
		GDcargo.addMarines((int) (Math.random() * 300) + 400);
		GDcargo.addFuel((int) (Math.random() * 1500) + 1500);
		GDcargo.addCrew(CargoAPI.CrewXPLevel.GREEN,
				(int) (Math.random() * 300) + 200);
		GDcargo.addCrew(CargoAPI.CrewXPLevel.REGULAR,
				(int) (Math.random() * 120) + 60);
		GDcargo.addCrew(CargoAPI.CrewXPLevel.VETERAN,
				(int) (Math.random() * 40) + 20);

		// gedune steal stuff, we assume they've stolen some stuff already
		List weaponIds = sector.getAllWeaponIds();
		for (int i = 0; i < 7; i++) {
			String weaponId = (String) weaponIds
					.get((int) (weaponIds.size() * Math.random()));
			int quantity = (int) (Math.random() * 3f + 1f);
			GDcargo.addWeapons(weaponId, quantity);
		}

		FactoryAPI fac = Global.getFactory();
		FleetDataAPI GDmoths = GDcargo.getMothballedShips();
		String[] ships = new String[] { "gedune_chua_wing", "gedune_chua_wing",
				"gedune_duri_wing", "gedune_duri_wing", "gedune_duri_wing" };
		for (int i = 0; i < ships.length; i++)
			GDmoths.addFleetMember(fac.createFleetMember(
					FleetMemberType.FIGHTER_WING, ships[i]));

		ships = new String[] { "gedune_tenzen", "gedune_nanda", "gedune_nanda",
				"gedune_nanda", "gedune_mozok", "gedune_mozok_t",
				"gedune_mozok_f", "gedune_mozok_f", "gedune_kitsune",
				"gedune_kitsune" };
		for (int i = 0; i < ships.length; i++)
			GDmoths.addFleetMember(fac.createFleetMember(FleetMemberType.SHIP,
					ships[i] + "_Hull"));

		FactionAPI gedune = sector.getFaction("gedune");
		// Gedune national policy: "EVERYTHING MUST GO!"
		String[] peopleIhate = new String[] { "hegemony", "tritachyon",
				"pirates", "pirates", "independent"/* , "player" */};
		for (int i = 0; i < peopleIhate.length; i++)
			gedune.setRelationship(peopleIhate[i], -1); // I hate you

		// peopleIhate = new String[] { ... }; //This is the normal way to do it
		String[] peopleIhate2HATEHARDER = { "interstellarFederation",
				"junk_pirates", "lotus_pirates", "nomads", "serenitystation",
				"antediluvian", "ghost", "neutrinocorp", "syndicate_asp",
				"shadow_industry" }; // This is funnier.
		for (int i = 0; i < peopleIhate2HATEHARDER.length; i++)
			gedune.setRelationship(peopleIhate2HATEHARDER[i], -1);

		// incidentally, you can set any relationship you want, with anybody,
		// even people that don't exist. I can't believe that doesn't crash!
		// Alex, Bro, THAT IS WHAT I CALL GRACEFUL FAILURE!
	}

}
