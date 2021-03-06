package com.example.friendzone

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.friendzone.manager.ApiManager
import com.example.friendzone.manager.GroupManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_user_content.*
import java.util.*
import java.util.function.Consumer
import kotlin.collections.HashMap


class UserContent : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    private var backPressTime: Long? = null
    private lateinit var backToast: Toast

    private lateinit var groupAdapter: GroupAdapter
    var listOfGroups = mutableListOf<String>()
    var groupMembers = mutableListOf<String>()
    private lateinit var groupManager: GroupManager
    var groupMap = HashMap<String, List<String>>()

    lateinit var apiManager: ApiManager

    companion object {
        const val USER_KEY = "USER_KEY"
        const val USER_DISPLAY_NAME = "USER_DISPLAY_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_content)

        btnAdd.alpha = .5f

        apiManager = (application as FriendZoneApp).apiManager

        backPressTime = 0
        backToast = Toast.makeText(this,"Press back again to exit",Toast.LENGTH_LONG);

        auth = (application as FriendZoneApp).auth

        currentUser = intent.getParcelableExtra<FirebaseUser>(USER_KEY)!!
        if (currentUser.displayName != null) {
            tvUser.text = currentUser.displayName
        } else {
            tvUser.text = intent.getStringExtra(USER_DISPLAY_NAME)?.toString()
        }

        btnLogout.setOnClickListener {
            logout()
        }


        btnAdd.setOnClickListener {
            btnAdd.alpha = 1f
            showAddItemDialog()
        }

        groupManager = (application as FriendZoneApp).groupManager

        if (!(application as FriendZoneApp).internetManager.isNetworkAvailable(this)) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show()
        } else {
            val userGroupsDatabaseRef =
                (application as FriendZoneApp).lookupDatabaseRef.child("user")
                    .child(currentUser.uid).child("groupList")

            userGroupsDatabaseRef.addValueEventListener(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (ds in dataSnapshot.children) {
//                    val user = ds.getValue(UserGroup::class.java)
                        listOfGroups.add(ds.value.toString())
                    }

//                    (application as FriendZoneApp).userGroups = listOfGroups

                    var groupQueue: Queue<String> = LinkedList(listOfGroups)
                    fetchNext(groupQueue)

//                    val membersDatabaseRef = (application as FriendZoneApp).lookupDatabaseRef.child("groups")
//                    for (group in listOfGroups) {
//
//                        Log.i("JY", group)
//                        val eachGroup = membersDatabaseRef.child(group).child("members")
//                        eachGroup.addValueEventListener(object : ValueEventListener {
//                            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                                for (ds in dataSnapshot.children) {
//                                    groupMembers.add(ds.value.toString())
//                                }
//                                groupMap[group] = groupMembers
//                                Log.i("JY", groupMap.toString())
//                            }
//
//                            override fun onCancelled(p0: DatabaseError) {
//                                Toast.makeText(
//                                    this@UserContent,
//                                    "Cannot retrieve user group members information",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//
//                        })
//                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(
                        this@UserContent,
                        "Cannot retrieve user group information",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

        }

//        groupManager.getGroups({ groupList ->
//            listOfGroups = groupList.groupList.toMutableList()
//            groupAdapter = GroupAdapter(listOfGroups)
//            groupAdapter.onGroupClickListener = {selectedGroup: Group ->
//                val intent = Intent(this, GroupFeed::class.java)
//                startActivity(intent)
//            }
//            rvGroups.adapter = groupAdapter
//        }, {
//            Log.i("info", "Error fetching groups")
//        })

    }

    private fun showAddItemDialog() {
        val taskEditText = EditText(this)

        val dialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle("Create new group")
            .setMessage("What's the name of new group?")
            .setView(taskEditText)
            .setPositiveButton("Create"
            ) { _, _ ->
                val newGroup = taskEditText.text.toString()
                Log.i("test", currentUser.email!! + " " + newGroup)
                if (newGroup.isNotEmpty()) {
                    apiManager.addUser(currentUser.email!!, newGroup, {

//                    Toast.makeText(this, "added $user to $group", Toast.LENGTH_SHORT).show()
                    }, {
//                    Toast.makeText(this, "Fail to added $user to $group", Toast.LENGTH_SHORT).show()
                    },
                        this,
                        true
                    )
                }
                btnAdd.alpha = .5f

            }
            .setNegativeButton("Cancel") { _, _ -> btnAdd.alpha = .5f}
            .create()

        dialog.setView(taskEditText, 50 ,0, 50 , 0)
        dialog.show()

    }

    private fun fetchNext(queue: Queue<String>) {
        val group = queue.poll()
        if (group != null) {
            apiManager.asyncFetch(groupMap, group, {
                fetchNext(queue)
            }, {
                Toast.makeText(this, "Cannot retrieve user group members information", Toast.LENGTH_SHORT).show()
            })
        } else {

            Log.i("JY", groupMap.toString())
//            Toast.makeText(this, "Finish getting group members information", Toast.LENGTH_SHORT).show()

            groupAdapter = GroupAdapter(groupMap)
            rvGroups.adapter = groupAdapter
            groupAdapter.onGroupClickListener = {
                val intent = Intent(this, GroupFeed::class.java)
                intent.putExtra("GROUP_NAME", it)
                startActivity(intent)
            }
        }

    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "User ${tvUser.text} logged out", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        btnAdd.alpha = .5f
    }

    override fun onBackPressed() {
        if (backPressTime?.plus(2000)!! > System.currentTimeMillis()) {
            backToast.cancel()
            super.onBackPressed()
            finishAffinity()
        } else {
            backToast.show()
        }
        backPressTime = System.currentTimeMillis()
    }
}
