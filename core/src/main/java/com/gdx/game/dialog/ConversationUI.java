package com.gdx.game.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.entities.EntityConfig;
import com.gdx.game.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;

import static com.gdx.game.common.Constats.COURTESY_PHRASES_PATH;
import static com.gdx.game.common.Constats.FOE;

public class ConversationUI extends Window {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationUI.class);

    private Label dialogText;
    private List listItems;
    private ConversationGraph graph;
    private String currentEntityID;
    private String currentEntityName; // added currentEntityName variable

    private TextButton closeButton;

    private Json json;

    private Random random = new Random();

    public ConversationUI() {
        super("dialog", ResourceManager.skin);

        json = new Json();
        graph = new ConversationGraph();

        //create
        dialogText = new Label("No Conversation", ResourceManager.skin);
        dialogText.setWrap(true);
        dialogText.setAlignment(Align.center);
        listItems = new List<ConversationChoice>(ResourceManager.skin);

        closeButton = new TextButton("X", ResourceManager.skin);
        closeButton.setName("closeButton");

        ScrollPane scrollPane = new ScrollPane(listItems, ResourceManager.skin);
        scrollPane.setName("scrollPane");
        scrollPane.setOverscroll(false, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setForceScroll(true, false);
        scrollPane.setScrollBarPositions(false, true);

        //layout
        this.add();
        this.add(closeButton);
        this.row();

        this.defaults().expand().fill();
        this.add(dialogText).pad(10, 10, 10, 10);
        this.row();
        this.add(scrollPane).pad(10,10,10,10);

        //this.debug();
        this.pack();

        //Listeners
        listItems.addListener(new ClickListener() {
            @Override
            public void clicked (InputEvent event, float x, float y) {
                ConversationChoice choice = (ConversationChoice) listItems.getSelected();
                if (choice == null) {
                    return;
                }
                graph.notify(graph, choice.getConversationCommandEvent());
                populateConversationDialog(choice.getDestinationId());
            }
        });
    }

    public void setTitle(String title) {
        this.getTitleLabel().setText(title);
    }

    public TextButton getCloseButton() {
        return closeButton;
    }

    public String getCurrentEntityID() {
        return currentEntityID;
    }

    public String getCurrentEntityName() { // added currentEntityName getter
        return currentEntityName;
    }

    public void loadConversation(EntityConfig entityConfig) {
        String fullFilenamePath = entityConfig.getConversationConfigPath();
		this.getTitleLabel().setText("");

        clearDialog();

        if(FOE.equalsIgnoreCase(entityConfig.getEntityStatus())){
            LOGGER.debug("The NPC is an Enemy");
            return;
        }

        if (fullFilenamePath.isEmpty() || !Gdx.files.internal(fullFilenamePath).exists()) {
            fullFilenamePath = COURTESY_PHRASES_PATH;
        }

        //shows entityName instead of entityID
        currentEntityID = entityConfig.getEntityID();
        currentEntityName = entityConfig.getEntityName();

        //if entityName is null
        //show ID instead
        if(currentEntityName == null){
            currentEntityName = currentEntityID;
        }
        this.getTitleLabel().setText(currentEntityName);

        ConversationGraph graph = json.fromJson(ConversationGraph.class, Gdx.files.internal(fullFilenamePath));

        // if the npc has nothing to say, use a random courtesy phrases
        if(graph.getCurrentConversationID() == null){
            int randomNumber = random.nextInt(graph.getConversations().size()) + 1;
            graph.setCurrentConversationID(String.valueOf(randomNumber));
        }

        setConversationGraph(graph);
    }

    public void loadResume(EntityConfig entityConfig, Array<String> drops) {
        String fullResumePath = entityConfig.getResumeConfigPath();
        String resume = fullResumePath
                .replace("<xp>", entityConfig.getEntityProperties().get(EntityConfig.EntityProperties.ENTITY_XP_REWARD.name()))
                .replace("<gold>", entityConfig.getEntityProperties().get(EntityConfig.EntityProperties.ENTITY_GP_REWARD.name()));
        if (!drops.isEmpty()) {
            String newLine = System.getProperty("line.separator");
            for (String drop : drops) {
                String dropResume = "Obtained : " + drop;
                resume = resume.concat(newLine).concat(dropResume);
            }
        }
        this.getTitleLabel().setText("");

        clearDialog();

        currentEntityID = entityConfig.getEntityID();
        dialogText.setText(resume);
    }

    public void loadUpgradeClass(String playerClass) {
        clearDialog();

        String resume = "Your character class was upgraded to " + playerClass;
        dialogText.setText(resume);
        LOGGER.info("Class upgraded to {}", playerClass);
    }

    public void setConversationGraph(ConversationGraph graph) {
        if (this.graph != null) {
            this.graph.removeAllObservers();
        }
        this.graph = graph;
        populateConversationDialog(this.graph.getCurrentConversationID());
    }

    public ConversationGraph getCurrentConversationGraph() {
        return this.graph;
    }

    private void populateConversationDialog(String conversationID) {
        clearDialog();

        Conversation conversation = graph.getConversationByID(conversationID);
        if (conversation == null) {
            return;
        }
        graph.setCurrentConversation(conversationID);
        dialogText.setText(conversation.getDialog());
        ArrayList<ConversationChoice> choices =  graph.getCurrentChoices();
        if (choices == null) {
            return;
        }
        listItems.setItems(choices.toArray());
        listItems.setSelectedIndex(-1);
    }

    private void clearDialog() {
        dialogText.setText("");
        listItems.clearItems();
    }

}
