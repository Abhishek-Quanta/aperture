package com.example.aperture

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private val viewModel:MovieViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        val tabView=findViewById<TabLayout>(R.id.tab_main)
        val navHostFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            as NavHostFragment
        val navController=navHostFragment.navController
        Log.i(TAG,"Activity Main Started")
        tabView.addOnTabSelectedListener(object:TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(tab?.text=="Movies"){
                    while(navController.currentDestination?.id!=R.id.movieListFragment){
                        navController.popBackStack()
                    }
                    navController.navigate(R.id.movieListFragment)
                }
                else if (tab?.text=="Peers"){
                    navController.navigate(R.id.wifiEnableFragment)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })
        viewModel.tabItem.observe(this){ tab->
            val i=if(tab==TabItem.MOVIES) 0 else 1
            tabView.selectTab(tabView.getTabAt(i))
        }
    }
}