package com.madmath.core.entity;

import com.madmath.core.animation.AnimationManager;
import com.madmath.core.resource.ResourceManager;
import com.madmath.core.screen.GameScreen;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;

public class MonsterFactory {

    ResourceManager manager;

    GameScreen gameScreen;

    static private int monstersNextId = 2000;

    public MonsterFactory(ResourceManager manager, GameScreen gameScreen){
        this.manager = manager;
        this.gameScreen = gameScreen;
    }

    @Nullable
    public Monster generateMonsterByName(String name) {
        Iterable<Monster> monsterIter = Monster.monsterSort.select(monster1 -> {
            try {
                return monster1.getClass().getField("alias").get(monster1) == name;
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            return false;
        });
        if(monsterIter.iterator().hasNext()){
            Monster monster = monsterIter.iterator().next();
            try {
                return monster.getClass().getConstructor(Integer.class, AnimationManager.class).newInstance(monstersNextId++,monster.getAnimationManager().clone());
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return Monster.monsterSort.get(0);
    }
}