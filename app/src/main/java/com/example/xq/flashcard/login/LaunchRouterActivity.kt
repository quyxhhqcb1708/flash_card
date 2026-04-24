package com.example.xq.flashcard.ui.login

import android.content.Intent
import android.os.Bundle
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityLaunchRouterBinding
import com.example.xq.flashcard.ui.main.MainActivity
import com.example.xq.flashcard.sync.StudyCloudSyncManager
import com.google.firebase.auth.FirebaseAuth

class LaunchRouterActivity : BaseActivity<ActivityLaunchRouterBinding>() {

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityLaunchRouterBinding {
        return ActivityLaunchRouterBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeToDestination()
    }

    private fun routeToDestination() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            GuestSessionStore.clear(this)
            StudyCloudSyncManager.bootstrapAfterLogin(this) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else if (GuestSessionStore.isGuestMode(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            startActivity(LoginActivity.createIntent(this))
            finish()
        }
    }
}
