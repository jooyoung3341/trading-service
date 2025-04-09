/**
 * 
 */
 function copyToClipboard(text) {
    // 클립보드에 복사
    navigator.clipboard.writeText(text).then(() => {
        console.error(text + " 복사됨!");
    }).catch(err => {
        console.error("복사 실패:", err);
    });
}