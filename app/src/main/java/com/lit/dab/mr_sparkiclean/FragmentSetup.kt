package com.lit.dab.mr_sparkiclean

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.support.v4.app.Fragment


class FragmentSetup: Fragment() {

    companion object {
        fun newInstance(): FragmentSetup {
            var fragmentSetup = FragmentSetup()
            var args = Bundle()
            fragmentSetup.arguments = args
            return fragmentSetup
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater!!.inflate(R.layout.fragment_setup, container, false)
        return rootView
    }

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view!!, savedInstanceState)

        var editTextHome = view!!.findViewById(R.id.editTextSetup) as EditText
    }
}