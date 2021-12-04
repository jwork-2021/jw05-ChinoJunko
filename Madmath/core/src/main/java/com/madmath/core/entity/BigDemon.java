package com.madmath.core.entity;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.madmath.core.animation.AnimationManager;
import com.madmath.core.resource.ResourceManager;
import com.madmath.core.screen.GameScreen;

public class BigDemon extends Monster{

    static public String alias = "big_demon";

    static public int TextureWidth = 32;
    static public int TextureHeight = 36;
    //default frameInterval :
    //    static public float FrameIdleInterval = 0.34f;
    //    static public float FrameRunInterval = 0.17f;

    public BigDemon(AnimationManager manager){
        super(manager);
    }

    public BigDemon(Integer id, AnimationManager manager){
        super(id, manager);
    }

    public BigDemon(Integer id, AnimationManager animationManager, GameScreen gameScreen, Vector2 position) {
        super(id, animationManager, gameScreen, position);
    }

    public BigDemon(Integer id, AnimationManager animationManager, GameScreen gameScreen) {
        super(id, animationManager, gameScreen);
    }

    @Override
    public void initSelf() {
        super.initSelf();
        box = new Rectangle(0,0,22,10);
        boxOffset = new Vector2(5,0);
        level = 4;
    }

}
