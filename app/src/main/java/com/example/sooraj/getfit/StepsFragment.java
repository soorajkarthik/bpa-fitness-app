package com.example.sooraj.getfit;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.sooraj.getfit.Model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;

public class StepsFragment extends Fragment {

    /**
     * Fields
     */
    private View view;
    private User user;
    private FirebaseDatabase database;
    private DatabaseReference users;
    private String username;
    private TextView stepsText, percentCompleted, distanceWalked, caloriesBurnedText;
    private ProgressBar progressBar;

    /**
     * Get reference to Firebase Database, and the "Users" node
     * Get reference to current user from the current activity
     * Get reference to all components of the fragment's view
     * @param inflater the LayoutInflater used by the MainActivity
     * @param container ViewGroup that this fragment is a part of
     * @param saveInstanceState the last saved state of the application
     * @return the view corresponding to this fragment
     */
   @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState) {

       database = FirebaseDatabase.getInstance();
       users = database.getReference("Users");
       user = ((MainActivity) getActivity()).getUser();
       username = user.getUsername();

        setHasOptionsMenu(true);
        view = inflater.inflate(R.layout.fragment_steps, container, false);
        stepsText = view.findViewById(R.id.stepsText);
        progressBar = view.findViewById(R.id.stepsProgressBar);
        distanceWalked = view.findViewById(R.id.distanceWalkedText);
        caloriesBurnedText = view.findViewById(R.id.caloriesBurnedText);
        percentCompleted = view.findViewById(R.id.percentOfStepGoalText);
        return view;
    }

    /**
     * Inflates options menu
     * @param menu Menu used by the current activity
     * @param inflater MenuInflater used by the current activity
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_steps, menu);
        updateDisplay();
    }

    /**
     * Inflates dialog from which user can change their step goal
     * @param item the item selected by the user
     * @return true because there is no need for system processing, all processing necessary processing is done in the method
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_steps) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Edit Step Goal");

            View viewInflated = LayoutInflater.from(getContext())
                        .inflate(R.layout.edit_stepgoal_dialog,
                            (ViewGroup) view,
                            false);

            final EditText input = viewInflated.findViewById(R.id.newStepGoal);
            builder.setView(viewInflated);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                /**
                 * If user has entered a new step goal, update user's step goal in Firebase
                 * @param dialog the dialog that received the click
                 * @param which the button that was clicked
                 */
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    if (!input.getText().toString().isEmpty()) {

                        dialog.dismiss();
                        int newStepGoal = Integer.parseInt(input.getText().toString());
                        user.setStepGoal(newStepGoal);
                        users.child(username).child("stepGoal").setValue(user.getStepGoal());
                        updateDisplay();
                    }
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                /**
                 * Closes dialog
                 * @param dialog the dialog that received the click
                 * @param which the button that was clicked
                 */
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.cancel();
                }
            });

            builder.show();
        }

        return true;
    }


    /**
     * Updates progress bar and text to match values stored in Firebase
     * Animates progress bar
     * Calculates amount of calories burned by user based on steps taken and updates value in Firebase
     */
    public void updateDisplay() {

        int percent = (user.getSteps() * 100) / user.getStepGoal();
        double milesWalked = (user.getSteps() * ((user.getHeight() * 0.413) / 12)) / 5280;
        DecimalFormat df = new DecimalFormat("0.00");
        String milesWalkedString = df.format(milesWalked);
        int caloriesBurned = (int) (0.4 * user.getWeight() * milesWalked);

        percentCompleted.setText(percent + "% of Goal");
        distanceWalked.setText(milesWalkedString + " Miles Walked");
        caloriesBurnedText.setText(caloriesBurned + " Calories Burned");
        stepsText.setText("" + user.getSteps());

        progressBar.setProgress(0);

        ObjectAnimator.ofInt(progressBar, "progress",
                ((user.getSteps() * 10000) / user.getStepGoal()))
                .setDuration(1000).start();

        user.setCaloriesBurned(caloriesBurned);
        users.child(username).child("caloriesBurned").setValue(user.getCaloriesBurned());
    }

}
