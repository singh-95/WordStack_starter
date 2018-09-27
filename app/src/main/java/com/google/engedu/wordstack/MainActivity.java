/* Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.engedu.wordstack;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private static final int WORD_LENGTH = 5;
    public static final int LIGHT_BLUE = Color.rgb(176, 200, 255);
    public static final int LIGHT_GREEN = Color.rgb(200, 255, 200);
    private ArrayList<String> words = new ArrayList<>();
    private Random random = new Random();
    private StackedLayout stackedLayout;
    private String word1, word2;
    private ViewGroup word1LinearLayout, word2LinearLayout;

    private Stack<LetterTile> placedTiles=new Stack<>();
    private Button undo;
    private String userWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        undo=(Button)findViewById(R.id.button);

        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("words.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while((line = in.readLine()) != null) {
                String word = line.trim();
                if(word.length()==WORD_LENGTH)
                    words.add(word);
            }
        } catch (IOException e) {
            Toast toast = Toast.makeText(this, "Could not load dictionary", Toast.LENGTH_LONG);
            toast.show();
        }
        LinearLayout verticalLayout = (LinearLayout) findViewById(R.id.vertical_layout);
        stackedLayout = new StackedLayout(this);
        verticalLayout.addView(stackedLayout, 3);

        word1LinearLayout = (ViewGroup) findViewById(R.id.word1);
        //word1LinearLayout.setOnTouchListener(new TouchListener());
        word1LinearLayout.setOnDragListener(new DragListener());
        word2LinearLayout = (ViewGroup) findViewById(R.id.word2);
        //word2LinearLayout.setOnTouchListener(new TouchListener());
        word2LinearLayout.setOnDragListener(new DragListener());
    }

    private class TouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !stackedLayout.empty()) {
                if(((ViewGroup)v).getChildCount()==WORD_LENGTH)
                    return false;
                LetterTile tile = (LetterTile) stackedLayout.peek();
                placedTiles.push(tile);
                tile.moveToViewGroup((ViewGroup) v);
                if (stackedLayout.empty()) {
                    undo.setEnabled(false);
                    TextView messageBox = (TextView) findViewById(R.id.message_box);
                    messageBox.setText(word1 + " " + word2);
                }
                return true;
            }
            return false;
        }
    }

    private class DragListener implements View.OnDragListener {

        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    v.setBackgroundColor(LIGHT_BLUE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundColor(LIGHT_GREEN);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(LIGHT_BLUE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(Color.WHITE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DROP:
                    // Dropped, reassign Tile to the target Layout
                    LetterTile tile = (LetterTile) event.getLocalState();
                    if(((ViewGroup)v).getChildCount()!=WORD_LENGTH) {
                        tile.moveToViewGroup((ViewGroup) v);
                        placedTiles.push(tile);
                        if (stackedLayout.empty()) {
                            undo.setEnabled(false);
                            TextView messageBox = (TextView) findViewById(R.id.message_box);
                            assert messageBox != null;
                            if(areTheseWords()) {
                                messageBox.setText(userWords);
                            }
                            else {
                                messageBox.setText(word1 + " " + word2);
                            }
                        }
                        return true;
                    }
            }
            return false;
        }
    }

    public boolean onStartGame(View view) {
        word1LinearLayout.removeAllViews();
        word2LinearLayout.removeAllViews();
        stackedLayout.clear();
        undo.setEnabled(true);
        placedTiles.clear();
        TextView messageBox = (TextView) findViewById(R.id.message_box);
        assert messageBox != null;
        messageBox.setText("Game started");
        int index1, index2;
        index2=random.nextInt(words.size());
        do{
            index1=random.nextInt(words.size());
        } while(index1==index2);
        word1=words.get(index1);
        word2=words.get(index2);
        StringBuilder scrambledWord=new StringBuilder(2*WORD_LENGTH);
        int i, j;
        for(i=0, j=0;i<WORD_LENGTH&&j<WORD_LENGTH;) {
            int choice=random.nextInt(100);
            if(choice<50) {
                scrambledWord.append(word1.charAt(i++));
            }
            else {
                scrambledWord.append(word2.charAt(j++));
            }
        }
        if(i<j)
            scrambledWord.append(word1.substring(i));
        else
            scrambledWord.append(word2.substring(j));
        LetterTile letterTiles[]=new LetterTile[2*WORD_LENGTH];
        for(i=0,j=scrambledWord.length()-1;i<2*WORD_LENGTH;i++, j--) {
            letterTiles[i] = new LetterTile(this, scrambledWord.charAt(j));
            stackedLayout.push(letterTiles[i]);
        }
        return true;
    }

    public boolean onUndo(View view) {
        if(placedTiles.isEmpty())
            return false;
        LetterTile tile=placedTiles.pop();
        tile.moveToViewGroup(stackedLayout);
        return true;
    }

    private boolean areTheseWords() {
        String inp1="", inp2="";
        for(int i=0;i<word1LinearLayout.getChildCount();i++) {
            LetterTile tile1=(LetterTile) word1LinearLayout.getChildAt(i);
            LetterTile tile2=(LetterTile) word2LinearLayout.getChildAt(i);
            inp1+=tile1.letter;
            inp2+=tile2.letter;
        }
        userWords=inp1+" "+inp2;
        return words.contains(inp1) && words.contains(inp2);
    }
}
