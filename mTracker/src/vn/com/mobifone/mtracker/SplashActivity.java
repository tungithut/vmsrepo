package vn.com.mobifone.mtracker;

import vn.com.mobifone.mtracker.common.Session;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SplashActivity extends Activity {

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Session.isLaunched()){
        	// Already launch, by pass to enter to Main Activity
        	
        	startActivity(new Intent(SplashActivity.this, VMSMainActivity.class));
            SplashActivity.this.finish();
            
        } else {
        	// Not yet launched, now launch the animation
        	setContentView(R.layout.splash);
        	startAnimating();
        	// Set the state of launching to TRUE
        	Session.setLaunched(true);
        }
    }

    /**
     * Helper method to start the animation on the splash screen
     */
    private void startAnimating() {
        // Fade in top title
        TextView logo1 = (TextView) findViewById(R.id.appTitle1);
        Animation fade1 = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo1.startAnimation(fade1);
        
        // Fade in bottom title after a built-in delay.
        TextView logo2 = (TextView) findViewById(R.id.appTitle2);
        Animation fade2 = AnimationUtils.loadAnimation(this, R.anim.fade_in2);
        logo2.startAnimation(fade2);
        
        // Transition to Main Menu when bottom title finishes animating
        fade2.setAnimationListener(new AnimationListener() {
        	
            public void onAnimationEnd(Animation animation) {
                // The animation has ended, transition to the Main Menu screen
                startActivity(new Intent(SplashActivity.this, VMSMainActivity.class));
                SplashActivity.this.finish();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });
        
        // Load animations for all views within the TableLayout
        Animation spinin = AnimationUtils.loadAnimation(this, R.anim.custom_anim);
        LayoutAnimationController controller = new LayoutAnimationController(spinin);
        
        /*TableLayout table = (TableLayout) findViewById(R.id.TableLayout01);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            row.setLayoutAnimation(controller);
        }*/
        ImageView imgCenter = (ImageView) findViewById(R.id.imageView1);
        imgCenter.setAnimation(spinin);
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the animation
        TextView logo1 = (TextView) findViewById(R.id.appTitle1);
        logo1.clearAnimation();
        TextView logo2 = (TextView) findViewById(R.id.appTitle2);
        logo2.clearAnimation();
        
        /*TableLayout table = (TableLayout) findViewById(R.id.TableLayout01);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            row.clearAnimation();
        }*/
        
        ImageView imgCenter = (ImageView) findViewById(R.id.imageView1);
        imgCenter.clearAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start animating at the beginning so we get the full splash screen experience
        startAnimating();
    }
	
}
