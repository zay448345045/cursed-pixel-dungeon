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

package com.shatteredpixel.yasd.general.actors.buffs;

import com.shatteredpixel.yasd.general.actors.blobs.Blizzard;
import com.shatteredpixel.yasd.general.actors.blobs.ConfusionGas;
import com.shatteredpixel.yasd.general.actors.blobs.CorrosiveGas;
import com.shatteredpixel.yasd.general.actors.blobs.Electricity;
import com.shatteredpixel.yasd.general.actors.blobs.Fire;
import com.shatteredpixel.yasd.general.actors.blobs.Freezing;
import com.shatteredpixel.yasd.general.actors.blobs.Inferno;
import com.shatteredpixel.yasd.general.actors.blobs.Miasma;
import com.shatteredpixel.yasd.general.actors.blobs.ParalyticGas;
import com.shatteredpixel.yasd.general.actors.blobs.Regrowth;
import com.shatteredpixel.yasd.general.actors.blobs.SmokeScreen;
import com.shatteredpixel.yasd.general.actors.blobs.StenchGas;
import com.shatteredpixel.yasd.general.actors.blobs.StormCloud;
import com.shatteredpixel.yasd.general.actors.blobs.ToxicGas;
import com.shatteredpixel.yasd.general.actors.blobs.Web;
import com.shatteredpixel.yasd.general.actors.mobs.NewTengu;
import com.shatteredpixel.yasd.general.messages.Messages;
import com.shatteredpixel.yasd.general.sprites.CharSprite;
import com.shatteredpixel.yasd.general.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class BlobImmunity extends FlavourBuff {
	
	{
		type = buffType.POSITIVE;
	}
	
	public static final float DURATION	= 20f;
	
	@Override
	public int icon() {
		return BuffIndicator.IMMUNITY;
	}
	
	@Override
	public void tintIcon(Image icon) {
		greyIcon(icon, 5f, cooldown());
	}
	
	@Override
	public String toString() {
		return Messages.get(this, "name");
	}

	{
		//all harmful blobs
		immunities.add( Blizzard.class );
		immunities.add( ConfusionGas.class );
		immunities.add( CorrosiveGas.class );
		immunities.add( Electricity.class );
		immunities.add( Fire.class );
		immunities.add( Freezing.class );
		immunities.add( Inferno.class );
		immunities.add( ParalyticGas.class );
		immunities.add( Regrowth.class );
		immunities.add( SmokeScreen.class );
		immunities.add( StenchGas.class );
		immunities.add( StormCloud.class );
		immunities.add( ToxicGas.class );
		immunities.add( Web.class );
		immunities.add( Miasma.class );

		immunities.add(NewTengu.FireAbility.FireBlob.class);
		immunities.add(NewTengu.BombAbility.BombBlob.class);
	}

	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add( CharSprite.State.PURITY );
		else if (target.invisible == 0) target.sprite.remove( CharSprite.State.PURITY );
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}
}
