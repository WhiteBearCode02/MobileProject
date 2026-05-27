package com.example.mobileproject.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileproject.R
import com.example.mobileproject.data.TravelRecord

/**
 * [TravelAdapter]: [개선] ListAdapter와 DiffUtil을 사용하여 RecyclerView의 성능을 최적화합니다.
 * 데이터 변경 시 전체 목록을 새로고침하는 대신, 변경된 항목만 지능적으로 업데이트하여 효율성을 극대화합니다.
 */
class TravelAdapter(
    private val onItemClick: (TravelRecord) -> Unit,         // 항목 클릭 리스너 (상세 전이 브릿지)
    private val onItemLongClick: (TravelRecord, View) -> Unit // 항목 롱클릭 리스너 (다이얼로그 소거 블릿지)
) : ListAdapter<TravelRecord, TravelAdapter.TravelViewHolder>(TravelDiffCallback()) {

    // item_travel 레이아웃 인플레이션 및 원자적 뷰홀더 인스턴스 팩토리 가동 시점
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel, parent, false)
        return TravelViewHolder(view)
    }

    // 생성된 고성능 뷰홀더에 물리적 배열 인덱스(position) 데이터를 결합 인입하는 시점
    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onItemLongClick)
    }

    /**
     * [TravelViewHolder]: 개별 메모리 셀 뷰의 컴포넌트 포인터를 캐싱 보존하여
     * 런타임 인플레이션 오버헤드를 제어하는 고성능 뷰홀더 아키텍처 클래스.
     */
    class TravelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // [식별자 주소 정합 완료]: 교정된 item_travel.xml 레이아웃 컴포넌트 ID 명세와 100% 무결하게 일치하도록 뷰바인딩 포인터를 수정했습니다.
        private val tvItemPlace: TextView = itemView.findViewById(R.id.tvItemPlace)
        private val tvItemDate: TextView = itemView.findViewById(R.id.tvItemDate)
        private val tvItemMemo: TextView = itemView.findViewById(R.id.tvItemMemo)

        fun bind(
            record: TravelRecord,
            onItemClick: (TravelRecord) -> Unit,
            onItemLongClick: (TravelRecord, View) -> Unit
        ) {
            // 정형 도메인 모델(no, place, visitDate 등)의 속성 스트림 매핑
            tvItemPlace.text = record.place
            tvItemDate.text = record.visitDate

            // [AI 해시태그 및 메모 융합 결합 알고리즘]
            // 온디바이스 TFLite 추론 가산점 명세인 해시태그 데이터 유무를 파싱하여,
            // 사용자가 입력한 추억 메모 본문 뒤에 이쁘게 개행 후 결합 인쇄되도록 가공 정합했습니다.
            if (record.hashtag.isNotEmpty()) {
                tvItemMemo.text = "${record.memo}\n\n🤖 AI 태그: ${record.hashtag}"
            } else {
                tvItemMemo.text = record.memo
            }

            // [단일 터치 제스처 명세]: 단발성 클릭 이벤트 가동 시 상세보기 화면 유도 익명 콜백 트리거
            itemView.setOnClickListener { onItemClick(record) }

            // [롱클릭 컨텍스트 메뉴 명세]: 2초 이상 롱터치 제스처 포착 시 가산점 요건인 AlertDialog 파괴 트랜잭션 수립
            itemView.setOnLongClickListener {
                onItemLongClick(record, itemView)
                true // [이벤트 버스 소비]: 하부 터치 컨텍스트 체인을 차단하여 일반 온클릭과 롱클릭이 경합 오동작하는 현상을 원천 방어
            }
        }
    }

    /**
     * [개선] DiffUtil.ItemCallback을 구현하여 두 TravelRecord 객체 간의 차이를 계산합니다.
     * 이를 통해 ListAdapter가 어떤 항목이 추가, 제거 또는 변경되었는지 효율적으로 판단할 수 있습니다.
     */
    class TravelDiffCallback : DiffUtil.ItemCallback<TravelRecord>() {
        override fun areItemsTheSame(oldItem: TravelRecord, newItem: TravelRecord): Boolean {
            // 각 아이템의 고유 ID(no)를 비교하여 동일한 아이템인지 확인
            return oldItem.no == newItem.no
        }

        override fun areContentsTheSame(oldItem: TravelRecord, newItem: TravelRecord): Boolean {
            // 아이템의 내용(데이터)이 동일한지 확인
            return oldItem == newItem
        }
    }
}