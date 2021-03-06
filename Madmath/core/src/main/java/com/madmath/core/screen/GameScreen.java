/**
*   @Author: Junko
*   @Email: imaizumikagerouzi@foxmail.com
*   @Date: 4/12/2021 下午3:57
*/
package com.madmath.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Sort;
import com.madmath.core.entity.Entity;
import com.madmath.core.entity.creature.Monster;
import com.madmath.core.entity.creature.MonsterFactory;
import com.madmath.core.entity.creature.Player;
import com.madmath.core.entity.obstacle.Obstacle;
import com.madmath.core.inventory.Item;
import com.madmath.core.inventory.equipment.Equipment;
import com.madmath.core.inventory.equipment.EquipmentFactory;
import com.madmath.core.main.MadMath;
import com.madmath.core.map.GameMap;
import com.madmath.core.entity.obstacle.ObstacleFactory;
import com.madmath.core.resource.ResourceManager;
import com.madmath.core.thread.MonsterThread;
import com.madmath.core.thread.PlayerThread;
import com.madmath.core.ui.HUD;
import com.madmath.core.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;

public class GameScreen extends AbstractScreen{

    static private GameScreen CurrencyGameScreen;

    HUD hud;

    public GameMap map;

    InputMultiplexer multiplexer;

    ExecutorService executorService;

    public Semaphore playerSemaphore;
    public Semaphore monsterSemaphore;
    public Semaphore playerFinishSemaphore;
    public Semaphore monstersFinishSemaphore;

    public Player player;
    public MonsterThread monsterManager;

    private int battleLevel;

    private MonsterFactory monsterFactory;
    private EquipmentFactory equipmentFactory;
    private ObstacleFactory obstacleFactory;

    Label currencyMapMessage;


    //collision detection
    public Array<Entity> livingEntity;
    public Array<Item> livingItem;

    float stateTime;
    float currencyDelta;
    float factor=1f;


    public GameScreen(final MadMath game, final ResourceManager manager){
        super(game, manager);

        CurrencyGameScreen = this;

        livingEntity = new Array<>();
        livingItem = new Array<>();

        monsterFactory = new MonsterFactory(manager,this);
        monsterManager = new MonsterThread();

        equipmentFactory = new EquipmentFactory(manager, this);

        obstacleFactory = new ObstacleFactory(manager, this);

        multiplexer = new InputMultiplexer();
        hud = new HUD(this, manager);
        multiplexer.addProcessor(hud.getStage());
        multiplexer.addProcessor(stage);

        playerSemaphore = new Semaphore(0);
        monsterSemaphore = new Semaphore(0);
        playerFinishSemaphore = new Semaphore(0);
        monstersFinishSemaphore = new Semaphore(0);

        executorService = Executors.newCachedThreadPool();
        executorService.execute(new PlayerThread());
        executorService.execute(monsterManager);
        executorService.shutdown();

        setBattleLevel(1);
    }

    public void setBattleLevel(int battleLevel) {
        this.battleLevel = battleLevel;
        music = manager.levelMusic[battleLevel-1];
    }

    @Override
    public void show() {
        if(state==State.READY){
            resetMap();
            player.freshSelf();
            new GameMap(this,"PRIMARY",1,factor);
            initMapTitle();
        }
        super.show();
        hud.show();
        currencyMapMessage.setPosition(stage.getViewport().getWorldWidth()/2-50, MadMath.V_HEIGHT-20);
        currencyMapMessage.setZIndex(1000);
        state = State.RUNING;
        CurrencyGameScreen = this;
        Gdx.input.setInputProcessor(multiplexer);
        stateTime = 0;
        camera.zoom =  0.7f;
    }

    public void nextMap(){
        int tlevel = map.mapLevel;
        resetMap();
        String s = "PRIMARY";
        if(tlevel>5) s = "ADVANCED";
        if(tlevel>10) s = "CRAZY";
        new GameMap(this,s,tlevel+1,factor);
        initMapTitle();
        currencyMapMessage.setPosition(stage.getViewport().getWorldWidth()/2-50, MadMath.V_HEIGHT-20);
        currencyMapMessage.setZIndex(1000);
        player.setPosition(map.playerSpawnPoint);
    }

    public void resetMap(){
        if(map!=null){
            map.dispose();
            monsterManager.monsters.forEach(entity -> {stage.getActors().removeValue(entity,true);monsterManager.removeMonster(entity);});
            livingEntity.forEach(entity -> {
                if(entity instanceof Obstacle){
                    stage.getActors().removeValue(entity,true);
                }
            });
            livingEntity.clear();
            livingItem.forEach(item -> {stage.getActors().removeValue(item,true);});
            livingItem.clear();
            livingEntity.add(player);
        }
    }

    public void initMapTitle(){
        if(currencyMapMessage!=null)    stage.getActors().removeValue(currencyMapMessage,true);
        currencyMapMessage = new Label("LEVEL "+map.mapLevel+"  "+map.name, new Label.LabelStyle(manager.font, Color.YELLOW ));
        currencyMapMessage.setFontScale(0.5f);
        currencyMapMessage.setVisible(true);
        currencyMapMessage.addAction(Actions.sequence(Actions.delay(5f),Actions.run(() -> currencyMapMessage.setText("   GOOD LUCK!   "))));
        stage.addActor(currencyMapMessage);
    }

    public void resetGame(){
        state = State.READY;
        player.weapon.forEach(weapon -> {stage.getActors().removeValue(weapon,true);});
        player.weapon.clear();
        player.activeWeapon = null;
        hud.reset();
    }

    public void updateCamera(){
        if(Math.abs(camera.zoom-1f)>0.01f) {
            camera.position.x = Math.min(Math.max(player.getX()+(0.5f-0.382f)*camera.zoom*stage.getViewport().getWorldWidth(), 0.5f * camera.zoom * stage.getViewport().getWorldWidth()),map.playAreaSize.x-0.5f* camera.zoom*stage.getViewport().getWorldWidth());
            camera.position.y = Math.min(Math.max(player.getY()-(0.5f-0.382f)*camera.zoom*stage.getViewport().getWorldHeight(), 0.5f * camera.zoom * stage.getViewport().getWorldHeight()),(1-0.5f* camera.zoom)*stage.getViewport().getWorldHeight());
        }else {
            camera.position.x = Math.min(Math.max(player.getX()+(0.5f-0.382f)*stage.getViewport().getWorldWidth(), (float) stage.getViewport().getWorldWidth()/2),map.startPosition.x+map.playAreaSize.x-stage.getViewport().getWorldWidth()/2);
            camera.position.y = viewport.getWorldHeight()/2f;
        }
        camera.update();
        //System.out.println("Playerx:"+player.getX()+"    viewportwith:"+camera.viewportWidth+"    playerArs:"+map.playAreaSize.x);
    }


    @Override
    public void resize(int i, int i1) {
        super.resize(i, i1);
        hud.resize(i,i1);
    }

    @Override
    public void update(float v) {
        super.update(v);
        updateCamera();
    }

    @Override
    public void render(float v) {
        super.render(v);
        stateTime += v;
        currencyDelta = v;
        playerSemaphore.release();
        monsterSemaphore.release();
        update(v);
        map.render(v);
        Sort.instance().sort(stage.getRoot().getChildren(), (o2, o1) -> (int)(o1.getUserObject()==null?
                (o2.getUserObject()==null?
                        (Float.compare(o1.getY(), o2.getY())): (Float.compare(o1.getY(), o2.getY() + (int) o2.getUserObject()))):
                o2.getUserObject()==null?
                        (Float.compare(o1.getY()+(int)o1.getUserObject(),o2.getY())):(Float.compare(o1.getY()+(int)o1.getUserObject(),o2.getY()+(int)o2.getUserObject()))));
        for (int i = monsterManager.monsters.size-1; i >= 0 ; i--) {
            if(monsterManager.monsters.get(i).getHp()<=0) monsterManager.monsters.get(i).Die();
        }
        if(player.getHp()<=0) player.Die();
        stage.act(v);
        stage.draw();
        hud.render(v);
    }

    public void createMonsters(float factor) {
        if(map==null)   return;
        int totalLevel = Math.round((map.mapLevel*16 + 32) * factor);
        int capacity = 500;
        Random random = new Random(System.currentTimeMillis());
        int monsterId = random.nextInt(Monster.monsterSort.size);
        for (int i = 0; i < capacity && totalLevel>0; monsterId = random.nextInt(Monster.monsterSort.size)) {
            try{
                if(Monster.monsterSort.get(monsterId).level>(2+totalLevel)/3)  continue;
                totalLevel -= Objects.requireNonNull(generateMonster((String) Monster.monsterSort.get(monsterId).getClass().getField("alias").get(null))).level;
                i++;
            } catch (NoSuchFieldException | IllegalAccessException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    public Monster generateMonster(String name) throws TimeoutException {//alias
        Monster monster = monsterFactory.generateMonsterByName(name);
        monster.setPosition(map.getAvailablePosition(monster));
        monsterManager.addMonster(monster);
        stage.addActor(monster);
        //Label label = new Label("",new Label.LabelStyle(manager.font,Color.YELLOW));
        //label.
        //monster.setZIndex((int) monster.getY());
        livingEntity.add(monster);
        return monster;
    }

    public EquipmentFactory getEquipmentFactory() {
        return equipmentFactory;
    }

    public ObstacleFactory getObstacleFactory(){
        return obstacleFactory;
    }

    public void createEquipment(){
        for (int i = 0; i < Utils.AllDefaultEquipmentSort.length; i++) {
            Equipment equipment = equipmentFactory.generateEquipmentByName(Utils.AllDefaultEquipmentSort[i]);
            equipment.setPosition(map.playerSpawnPoint.x+50+50*(i/2),map.playerSpawnPoint.y+50-100*(i%2));
            stage.addActor(equipment);
            livingItem.add(equipment);
        }
    }

    public GameMap getMap() {
        return map;
    }

    public float getCurrencyDelta() {
        return currencyDelta;
    }

    public void changeDifficulty(float factor){
        this.factor = factor;
    }

    @Override
    public void switchScreen(Screen screen) {
        super.switchScreen(screen);
        // getViewport().update();

    }



    public void addInputProcessor(InputProcessor inputProcessor){
        multiplexer.addProcessor(0,inputProcessor);
    }

    public static GameScreen getCurrencyGameScreen() {
        return CurrencyGameScreen;
    }

}
