package com.example.mobileproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mobileproject.R

class InfoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 앱 정보 레이아웃을 전개합니다.
        return inflater.inflate(R.layout.fragment_info, container, false)
    }
}
