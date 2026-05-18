package com.example.mobileproject.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileproject.R
import com.example.mobileproject.data.TravelRecord

/**
 * 여행 기록 목록을 관리하고 화면에 바인딩하는 커스텀 리사이클러뷰 어댑터
 * 교수님 필수 요구사항에 따라 Adapter와 ViewHolder를 직접 설계함
 */
class TravelAdapter(
    private var records: List<TravelRecord>,
    private val onItemClick: (TravelRecord) -> Unit,         // 항목 클릭 리스너 (상세 화면 이동용)
    private val onItemLongClick: (TravelRecord, View) -> Unit // 항목 롱클릭 리스너 (컨텍스트 메뉴용)
) : RecyclerView.Adapter<TravelAdapter.TravelViewHolder>() {

    // 외부에서 새로운 리스트를 주입해 목록을 갱신할 때 사용하는 메서드
    fun updateData(newRecords: List<TravelRecord>) {
        this.records = newRecords
        notifyDataSetChanged() // 구조적 변경 사항을 리사이클러뷰에 알림
    }

    // item_travel 레이아웃을 인플레이트하여 ViewHolder를 생성하는 시점
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel, parent, false)
        return TravelViewHolder(view)
    }

    // 생성된 ViewHolder에 특정 위치(position)의 데이터를 연결하는 시점
    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        holder.bind(records[position], onItemClick, onItemLongClick)
    }

    // 전체 아이템 개수를 반환
    override fun getItemCount(): Int = records.size

    /**
     * 개별 아이템 뷰의 컴포넌트들을 보유하고 데이터를 매핑하는 뷰홀더 클래스
     */
    class TravelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val tvPlace: TextView = itemView.findViewById(R.id.tvPlace)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvHashtag: TextView = itemView.findViewById(R.id.tvHashtag)

        fun bind(
            record: TravelRecord,
            onItemClick: (TravelRecord) -> Unit,
            onItemLongClick: (TravelRecord, View) -> Unit
        ) {
            tvPlace.text = record.place
            tvDate.text = record.visitDate

            // AI 해시태그가 비어있지 않다면 보이고, 비어있다면 숨김 처리하는 예외 방어코드
            if (record.hashtag.isNotEmpty()) {
                tvHashtag.visibility = View.VISIBLE
                tvHashtag.text = record.hashtag
            } else {
                tvHashtag.visibility = View.GONE
            }

            // 사진 URI 경로가 존재하면 이미지뷰에 바인딩, 없으면 기본 아이콘 적용
            if (record.photoUri.isNotEmpty()) {
                try {
                    ivThumbnail.setImageURI(Uri.parse(record.photoUri))
                } catch (e: Exception) {
                    // 갤러리 권한 만료 등의 이유로 로드 실패 시 디폴트 이미지 세팅
                    ivThumbnail.setImageResource(R.drawable.ic_launcher_background)
                }
            } else {
                ivThumbnail.setImageResource(R.drawable.ic_launcher_background)
            }

            // [요구사항] 1차 클릭 시 상세 보기 화면 유도 호출
            itemView.setOnClickListener { onItemClick(record) }

            // [요구사항] 길게 클릭 시 수정/삭제 컨텍스트 메뉴 생성을 위한 롱클릭 연결
            itemView.setOnLongClickListener {
                onItemLongClick(record, itemView)
                true // 이벤트 소비 완료를 뜻함 (일반 클릭이 중복 발생하지 않도록 방어)
            }
        }
    }
}