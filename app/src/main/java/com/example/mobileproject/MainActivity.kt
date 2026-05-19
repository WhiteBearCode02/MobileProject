package com.example.mobileproject

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mobileproject.ui.DashboardFragment
import com.example.mobileproject.ui.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * [MainActivity]: 하단 네비게이션바를 통해 2개의 프래그먼트(Home, Dashboard) 탭 전환 및
 * 백스택 트랜잭션을 소프트웨어 공학적으로 통제하는 최상위 엔트리 포인트 액티비티 계층.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        // [인프라 청정화]: 구글 맵 플랫폼 선회에 따라 무의미해진 카카오 서명(Signatures) 복잡 파싱 루틴을 완전히 청소하여 Null 상호 충돌 결함을 원천 타파합니다.

        // 앱 최초 실행 시 가상 메모리 스냅샷(savedInstanceState) 유무를 검증하여 디폴트 탭으로 HomeFragment를 생성 적재
        if (savedInstanceState == null) {
            switchFragment(HomeFragment(), "Home")
        }

        // 하단 탭 버튼 클릭 리스너 연결 및 분기 제어 알고리즘 수립
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(HomeFragment(), "Home")
                    true
                }
                R.id.nav_dashboard -> {
                    switchFragment(DashboardFragment(), "Dashboard")
                    true
                }
                else -> false
            }
        }

        // 안드로이드 최신 보안 및 가용성 규격(API 33+)에 맞춘 백스택 제어 전용 콜백 등록 (Legacy onBackPressed 대체)
        setupBackPressedDispatcher()
    }

    /**
     * [switchFragment]: 프래그먼트 매니저를 통해 컴포넌트를 원자적 트랜잭션으로 교체하고, 교수님 필수 요건인 백스택 트래킹을 수행합니다.
     * @param fragment 교체 전이할 서브 프래그먼트 인스턴스
     * @param tag 백스택 프레임 식별용 정적 문자열 기호
     */
    private fun switchFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager

        // 스택 메모리에 이미 컨텍스트가 잔존하는 프래그먼트가 있다면 중복 인스턴스 생성을 억제하기 위해 탐색 추출
        val currentFragment = fragmentManager.findFragmentByTag(tag)
        val transaction = fragmentManager.beginTransaction()

        // 독립형 fragment_container 뷰포트 레이어 상에서 뷰 교체 런타임 가동
        transaction.replace(R.id.fragment_container, currentFragment ?: fragment, tag)

        // [과제 필수 요구사항]: 백스택 트리거 설계를 포함하여 사용자가 하드웨어 뒤로가기 클릭 시 이전 활성 탭으로 안전 전이되도록 통제
        transaction.addToBackStack(tag)
        transaction.commit()
    }

    /**
     * [setupBackPressedDispatcher]: Jetpack 컴포넌트 아키텍처에 부합하는 독립형 백스택 콜백 처리 디스패처 가동 알고리즘.
     */
    private fun setupBackPressedDispatcher() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = supportFragmentManager

                // 백스택 세션에 적재된 프래그먼트 컨텍스트 레코드가 2개 이상 존재할 때 이전 탭 뷰포트로 안전 복원(Pop)
                if (fragmentManager.backStackEntryCount > 1) {
                    fragmentManager.popBackStackImmediate()
                    // 전이된 메모리 프래그먼트 상태에 호응하도록 하단 네비게이션 선택 탭 인덱스 동기화 갱신
                    updateBottomNavSelection()
                } else {
                    // 더 이상 전이할 백스택 리소스가 부재할 경우 본 옵저버 콜백 인터셉터를 해제하고 물리 OS 시스템 단으로 종료 프로토콜 토스
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        // 액티비티 컨텍스트 수명 주기(Lifecycle)와 결합하여 디스패처 인프라에 안전하게 콜백 레지스트리 등록
        onBackPressedDispatcher.addCallback(this, callback)
    }

    /**
     * [updateBottomNavSelection]: 하드웨어 백스택 뒤로가기 시 가상 파일 시스템 뷰포트에 렌더링된 객체를 추론하여 하단 탭 하이라이트를 보간하는 메서드.
     */
    private fun updateBottomNavSelection() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is HomeFragment) {
            bottomNavigation.selectedItemId = R.id.nav_home
        } else if (currentFragment is DashboardFragment) {
            bottomNavigation.selectedItemId = R.id.nav_dashboard
        }
    }
}