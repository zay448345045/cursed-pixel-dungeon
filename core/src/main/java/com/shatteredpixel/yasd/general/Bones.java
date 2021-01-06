/*
 *
 *  * Pixel Dungeon
 *  * Copyright (C) 2012-2015 Oleg Dolya
 *  *
 *  * Shattered Pixel Dungeon
 *  * Copyright (C) 2014-2019 Evan Debenham
 *  *
 *  * Cursed Pixel Dungeon
 *  * Copyright (C) 2014-2020 Samuel Braithwaite
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 *
 */

package com.shatteredpixel.yasd.general;

import com.shatteredpixel.yasd.general.actors.hero.Hero;
import com.shatteredpixel.yasd.general.items.Generator;
import com.shatteredpixel.yasd.general.items.Gold;
import com.shatteredpixel.yasd.general.items.Item;
import com.shatteredpixel.yasd.general.items.artifacts.Artifact;
import com.shatteredpixel.yasd.general.items.unused.missiles.MissileWeapon;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Bones {

	private static final String BONES_FILE	= "bones.dat";
	
	private static final String LEVEL	= "level";
	private static final String ITEM	= "item";

	private static int depth = -1;
	private static Item item;
	
	public static void leave() {

		depth = Dungeon.depth;

		//heroes which have won the game, who die far above their farthest depth, or who are challenged drop no bones.
		if (Statistics.amuletObtained || (Statistics.deepestFloor - 5) >= depth || Dungeon.challenges > 0) {
			depth = -1;
			return;
		}

		item = pickItem(Dungeon.hero);

		Bundle bundle = new Bundle();
		bundle.put( LEVEL, depth );
		bundle.put( ITEM, item );

		try {
			FileUtils.bundleToFile( BONES_FILE, bundle );
		} catch (IOException e) {
			CPDGame.reportException(e);
		}
	}

	private static Item pickItem(Hero hero){
		Item item = null;
		if (Random.Int(3) != 0) {
			if (Random.Int(6) == 0) {
				item = Dungeon.quickslot.randomNonePlaceholder();
			} else {
				item = Random.element(hero.belongings.miscs);
			}
			if (item == null || !item.bones) {
				return pickItem(hero);
			}
		} else {

			Iterator<Item> iterator = hero.belongings.backpack.iterator();
			Item curItem;
			ArrayList<Item> items = new ArrayList<>();
			while (iterator.hasNext()){
				curItem = iterator.next();
				if (curItem.bones)
					items.add(curItem);
			}

			if (Random.Int(3) < items.size()) {
				item = Random.element(items);
				if (item.stackable){
					item.quantity(Random.NormalIntRange(1, (item.quantity() + 1) / 2));
				}
			} else {
				if (Dungeon.gold > 100) {
					item = new Gold( Random.NormalIntRange( 50, Dungeon.gold/2 ) );
				} else {
					item = new Gold( 50 );
				}
			}
		}
		
		return item;
	}

	public static Item get() {
		if (depth == -1) {

			try {
				Bundle bundle = FileUtils.bundleFromFile(BONES_FILE);

				depth = bundle.getInt( LEVEL );
				item = (Item)bundle.get( ITEM );

				return get();

			} catch (IOException e) {
				return null;
			}

		} else {
			//heroes who are challenged cannot find bones
			if (depth == Dungeon.depth && Dungeon.challenges == 0) {
				FileUtils.deleteFile( BONES_FILE );
				depth = 0;
				
				if (item == null) return null;

				//Enforces artifact uniqueness
				if (item instanceof Artifact){
					if (Generator.removeArtifact(((Artifact)item).getClass())) {
						
						//generates a new artifact of the same type, always +0
						Artifact artifact = (Artifact) Reflection.newInstance(item.getClass());

						if (artifact == null){
							return new Gold(item.price());
						}

						artifact.curse();
						artifact.cursedKnown = true;

						return artifact;
						
					} else {
						return new Gold(item.price());
					}
				}
				
				if (item.isUpgradable() && !(item instanceof MissileWeapon)) {
					item.curse();
					item.cursedKnown = true;
				}
				
				if (item.isUpgradable()) {
					//caps at +3
					if (item.level() > 3) {
						item.degrade( item.level() - 3 );
					}
					//thrown weapons are always IDed, otherwise set unknown
					item.levelKnown = item instanceof MissileWeapon;
				}
				
				item.reset();
				
				return item;
			} else {
				return null;
			}
		}
	}
}
