package com.luseen.ribble.presentation.navigation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import com.luseen.ribble.R
import com.luseen.ribble.di.scope.PerActivity
import com.luseen.ribble.presentation.base_mvp.base.BaseFragment
import com.luseen.ribble.presentation.widget.navigation_view.NavigationId
import com.luseen.ribble.utils.inTransaction
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * Created by Chatikyan on 15.08.2017.
 */
@PerActivity
class Navigator @Inject constructor(private val activity: AppCompatActivity) : Router {

    interface NonRegistryFragmentListener {
        fun onNonRegistryFragmentOpen(tag: NavigationId) {
        }

        fun onNonRegistryFragmentClose() {
        }
    }

    interface TitleChangeListener {
        fun onTitleChanged(newTitle: String){
        }
    }

    private val fragmentManager: FragmentManager = activity.supportFragmentManager
    private var fragmentMap: MutableMap<String, Fragment> = mutableMapOf()

    private val containerId = R.id.container //TODO change
    var activeTag: String? = null
    var rootTag: String? = null
    lateinit var nonRegistryFragmentListener: NonRegistryFragmentListener
    lateinit var titleChangeListener: TitleChangeListener

    fun getState(): NavigationState {
        return NavigationState(activeTag, rootTag)
    }

    fun restore(state: NavigationState) {
        activeTag = state.activeTag
        rootTag = state.firstTag
        state.clear()

        fragmentMap.clear()
        fragmentManager.fragments
                .filter { !it.tag.contains("android") } //FiXME not the best solution
                .forEach {
                    fragmentMap.put(it.tag, it)
                }

        fragmentManager.inTransaction {
            fragmentMap
                    .filter { it.key != activeTag }
                    .forEach {
                        hide(it.value)
                    }
            show(fragmentMap[activeTag])
        }
        changeTitle(activeTag)
    }

    override fun goTo(kClass: KClass<out Fragment>, arg: Bundle) {
        val tag = kClass.java.name
        if (activeTag == tag)
            return

        if (!fragmentMap.containsKey(tag)) {
            val fragment = Fragment.instantiate(activity, tag)
            if (!arg.isEmpty) {
                fragment.arguments = arg
            }
            fragmentManager.inTransaction {
                add(containerId, fragment, tag)
            }
            fragmentMap.put(tag, fragment)

            if (activeTag == null) { //TODO
                rootTag = tag
            }
        }

        fragmentManager.inTransaction {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            fragmentMap
                    .filter { it.key != tag }
                    .forEach {
                        hide(it.value)
                    }
            show(fragmentMap[tag])
        }
        activeTag = tag
        changeTitle(tag)
    }

    private fun changeTitle(tag: String?) {
        val fragment = fragmentMap[tag]
        if (fragment is BaseFragment<*, *>) {
            titleChangeListener.onTitleChanged(fragment.getTitle())
        }
    }

    override fun hasBackStack(): Boolean {
        return fragmentMap.size > 1 && activeTag != rootTag
    }

    override fun goBack() {
        fragmentManager.inTransaction {
            remove(fragmentMap[activeTag])
        }
        fragmentMap.remove(activeTag)
        val currentTag = fragmentMap.keys.elementAt(fragmentMap.size - 1)

        fragmentManager.inTransaction {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            show(fragmentMap[currentTag])
        }
        activeTag = currentTag
        changeTitle(currentTag)
    }

    override fun goToFirst() {
        TODO()
    }
}