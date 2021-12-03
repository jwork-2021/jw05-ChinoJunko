package com.madmath.core.map;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;

public class AnimTile extends AnimatedTiledMapTile {
    public AnimTile(float interval, Array<StaticTiledMapTile> frameTiles) {
        super(interval, frameTiles);
    }

    public enum TileSort{
        floor_spikes_anim
    };
}