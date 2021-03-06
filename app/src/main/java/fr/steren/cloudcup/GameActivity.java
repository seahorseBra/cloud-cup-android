/*
Copyright 2014 Google Inc. All rights reserved.

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/

package fr.steren.cloudcup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import fr.steren.cloudcup.games.EndOfGameActivity;
import fr.steren.cloudcup.games.MathGameActivity;
import fr.steren.cloudcup.games.SequenceGameActivity;
import fr.steren.cloudcup.games.ShakingGameActivity;
import fr.steren.cloudcup.games.SwipeGameActivity;
import fr.steren.cloudcup.games.TappingGameActivity;
import fr.steren.cloudcup.games.TurnGameActivity;
import fr.steren.cloudcup.games.WaitingActivity;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.HashMap;


/**
 * Created by steren
 */
public class GameActivity extends Activity {
    private static final String LOG_TAG = GameActivity.class.getSimpleName();

    public enum GameState {
        WAITING, GAME, DONE
    }

    protected Firebase gameDataRef;
    protected String code;
    protected String playerID;
    protected Intent currentIntent;
    protected String gameType = "";
    protected String currentGame;
    protected GameState state;

    private Firebase gameTypeRef;


    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the room code
        code = getIntent().getStringExtra("code");
        Log.d(LOG_TAG, "Room code is " + code);

        // get the player ID
        playerID = getIntent().getStringExtra("playerId");
        Log.d(LOG_TAG, "Player ID is " + playerID);

        currentGame = getIntent().getStringExtra("number") != null ?
                getIntent().getStringExtra("number") : "-1";

        currentIntent = getIntent();

        Firebase.setAndroidContext(this);
        gameDataRef = new Firebase(Consts.FIREBASE_URL + "/room/" + code + "/games/" +
                currentGame + "/data/" + playerID);

        // reference to current game
        Firebase currentGameRef = new Firebase(Consts.FIREBASE_URL + "/room/" + code);

        // listen to state changes, if display a progress dialog if "waiting"
        currentGameRef.child("state").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    if(snapshot.getValue().toString().equals("waiting")
                            && !state.equals(GameState.WAITING)) {
                        openWaitingRoom();
                    } else if(snapshot.getValue().toString().equals("done")
                            && !state.equals(GameState.DONE)) {
                        openEndOfGameRoom();
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
            }
        });

        // listen to currentGame changes to change game activity
        currentGameRef.child("currentGame").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null
                        && !snapshot.getValue().toString().equals("-1")
                        && !currentGame.equals(snapshot.getValue().toString())) {
                    currentGame = snapshot.getValue().toString();
                    gameTypeRef = new Firebase(Consts.FIREBASE_URL + "/room/" + code + "/games/" + currentGame);
                    gameTypeRef.child("type").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null &&
                                    !dataSnapshot.getValue().toString().isEmpty()) {
                                Log.d(LOG_TAG, "gameType is now " + dataSnapshot.getValue());
                                gameType = (String) dataSnapshot.getValue();
                                startGame();
                            }
                        }
                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                        }
                    });
                    startGame();
                }
            }
            @Override public void onCancelled(FirebaseError error) {
            }
        });
    }

    private void startGame() {
        if (gameType == null || gameType.isEmpty()) return;
        if (currentGame.equals("-1")) return;
        if (currentIntent != null && currentIntent.getStringExtra("number") != null &&
                currentIntent.getStringExtra("number").equals(currentGame)) return;

        HashMap<String, Class> gameMapping = new HashMap<String, Class>();
        gameMapping.put("math", MathGameActivity.class);
        gameMapping.put("tap", TappingGameActivity.class);
        gameMapping.put("shake", ShakingGameActivity.class);
        gameMapping.put("swipe", SwipeGameActivity.class);
        gameMapping.put("turn", TurnGameActivity.class);
        gameMapping.put("sequence", SequenceGameActivity.class);

        Class cls = gameMapping.get(gameType);
        // temporary for testing
        //cls = TurnGameActivity.class;

        if(cls != null ) {
            openRoom(cls, playerID, code, currentGame);
        } else {
            Log.e(LOG_TAG, "Game Type unknown: " + gameType);
        }
    }

    private void openWaitingRoom() {
        openRoom(WaitingActivity.class, playerID, code, currentGame);
    }

    private void openEndOfGameRoom() {
        openRoom(EndOfGameActivity.class, playerID, code, currentGame);
    }

    private void openRoom(Class cls, String playerID, String code, String currentGame) {
        Intent intent = new Intent(this, cls);
        intent.putExtra("playerId", playerID);
        intent.putExtra("code", code);
        intent.putExtra("number", currentGame);
        startActivity(intent);
        finish();
    }
}