# project-lupin
## 검색 로직
1. 논문 이름 전체 (ex. 한글이름 : 부제 = english_name : subtitle)
2. 한글 이름만 (ex. 한글이름 : 부제)
3. 영어 이름만 (ex. english_name : subtitle)
4. 부제 제외한 한글 이름 (ex. 한글이름)
5. 부제 제외한 영어 이름 (ex. english_name)

## 결과 신뢰도 체크
### 검색 결과로 나온 논문 전체 이름과 검색어 비교
### Komoran으로 형태소 분석 후 자카드 유사도 비교
1. 논문 전체 이름 비교
2. 영어 한글 별개로 있다면 각각 체크
3. 부제가 있다면 빼고 각각 체크

## 속도 문제
### 각각 웹의 응답속도가 제한적이기 때문에 멀티스레드로 여러개 돌리는 방식 선택
### 노트북 와이파이 네트워크 환경에선 성능을 100퍼 발휘하지 못하는 것 같다


## 이슈 사항
### 멀티스레드로 전환 후 창을 여러개띄우고자 브라우저를 축소해서 배치했더니
### 기존 잘 돌던 것들에서 에러 발생
### 생각해보니 현재 CSS기준으로 크롤링 중인데 사이트가 반응형이라 CSS가 달라져서 그런것 같다.
### 항상 풀사이즈를 기준으로 하기로 함.
