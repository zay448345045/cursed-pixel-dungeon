/*
 *
 *  * Pixel Dungeon
 *  * Copyright (C) 2012-2015 Oleg Dolya
 *  *
 *  * Shattered Pixel Dungeon
 *  * Copyright (C) 2014-2019 Evan Debenham
 *  *
 *  * Yet Another Shattered Dungeon
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

package com.shatteredpixel.yasd.general.levels.features;

import com.shatteredpixel.yasd.general.Assets;
import com.shatteredpixel.yasd.general.Badges;
import com.shatteredpixel.yasd.general.Dungeon;
import com.shatteredpixel.yasd.general.Element;
import com.shatteredpixel.yasd.general.LevelHandler;
import com.shatteredpixel.yasd.general.actors.Char;
import com.shatteredpixel.yasd.general.actors.buffs.Bleeding;
import com.shatteredpixel.yasd.general.actors.buffs.Buff;
import com.shatteredpixel.yasd.general.actors.buffs.Cripple;
import com.shatteredpixel.yasd.general.actors.hero.Hero;
import com.shatteredpixel.yasd.general.actors.mobs.Mob;
import com.shatteredpixel.yasd.general.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.yasd.general.items.spells.FeatherFall;
import com.shatteredpixel.yasd.general.levels.RegularLevel;
import com.shatteredpixel.yasd.general.levels.rooms.Room;
import com.shatteredpixel.yasd.general.levels.rooms.special.WeakFloorRoom;
import com.shatteredpixel.yasd.general.messages.Messages;
import com.shatteredpixel.yasd.general.plants.Swiftthistle;
import com.shatteredpixel.yasd.general.scenes.GameScene;
import com.shatteredpixel.yasd.general.sprites.MobSprite;
import com.shatteredpixel.yasd.general.windows.WndOptions;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Chasm {

	public static boolean jumpConfirmed = false;
	
	public static void heroJump( final Hero hero ) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(
						new WndOptions( Messages.get(Chasm.class, "chasm"),
								Messages.get(Chasm.class, "jump"),
								Messages.get(Chasm.class, "yes"),
								Messages.get(Chasm.class, "no") ) {
							@Override
							protected void onSelect( int index ) {
								if (index == 0) {
									jumpConfirmed = true;
									hero.resume();
								}
							}
						}
				);
			}
		});
	}
	
	public static void heroFall( int pos ) {
		
		jumpConfirmed = false;
				
		Sample.INSTANCE.play( Assets.SND_FALLING );

		Buff buff = Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);
		if (buff != null) buff.detach();
		buff = Dungeon.hero.buff(Swiftthistle.TimeBubble.class);
		if (buff != null) buff.detach();
		
		if (Dungeon.hero.isAlive()) {
			Dungeon.hero.interrupt();
			boolean fallIntoPit;
			if (Dungeon.level instanceof RegularLevel) {
				Room room = ((RegularLevel)Dungeon.level).room( pos );
				fallIntoPit = room instanceof WeakFloorRoom;
			} else {
				fallIntoPit = false;
			}

			LevelHandler.fall(fallIntoPit, Dungeon.bossLevel());

		} else {
			Dungeon.hero.sprite.visible = false;
		}
	}
	
	public static void heroLand() {
		
		Hero hero = Dungeon.hero;
		
		FeatherFall.FeatherBuff b = hero.buff(FeatherFall.FeatherBuff.class);
		
		if (b != null){
			//TODO visuals
			b.detach();
			return;
		}
		
		Camera.main.shake( 4, 1f );

		Dungeon.level.occupyCell(hero );
		Buff.prolong( hero, Cripple.class, Cripple.DURATION );

		//The lower the hero's HP, the more bleed and the less upfront damage.
		//Hero has a 50% chance to bleed out at 66% HP, and begins to risk instant-death at 25%
		Buff.affect( hero, FallBleed.class).set( Math.round(hero.HT / (6f + (6f*(hero.HP/(float)hero.HT)))));
		hero.damage( Math.max( hero.HP / 2, Random.NormalIntRange( hero.HP / 2, hero.HT / 4 )), new Char.DamageSrc(Element.PHYSICAL, Chasm.class).ignoreDefense());
	}

	public static void mobFall( Mob mob ) {
		if (mob.isAlive()) mob.die( new Char.DamageSrc(Element.META, Chasm.class));
		
		((MobSprite)mob.sprite).fall();
	}
	
	public static class Falling extends Buff {
		
		{
			actPriority = VFX_PRIO;
		}
		
		@Override
		public boolean act() {
			heroLand();
			detach();
			return true;
		}
	}
	
	public static class FallBleed extends Bleeding implements Hero.Doom {
		
		@Override
		public void onDeath() {
			Badges.validateDeathFromFalling();
		}
	}
}
