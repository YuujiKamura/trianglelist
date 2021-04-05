/**
	All Rights Reserved,(c) JACIC 2001
	ComSplitStringC.h						作成日：1998.09.04

	１．クラス概要
		文字列分割クラス

	２．履歴
		履歴番号	日付け		担当者		概要
		作成		1998.09.04	直野(FS)

**/
#ifndef _COMSPLITSTRINGC_CLASS_
#define _COMSPLITSTRINGC_CLASS_

#include <afx.h>

class ComSplitStringC {
public :
//
//	<コンストラクタ>
//
	ComSplitStringC();
	ComSplitStringC(CString& IN_Str) {
		m_String = IN_Str ;
	}
	ComSplitStringC(CString* IN_Str) {
		m_String = *IN_Str ;
	}
	ComSplitStringC(char* IN_Str) {
		m_String = IN_Str ;
	}
//
//	<デストラクタ>
//
	~ComSplitStringC() {}
//
//	<指定された区切り文字コードで分割する>
//
	CStringArray*	Split(int	IN_SplitCode) {
		m_SplitData.SetSize(0) ;
		CString		str ;
		LPTSTR		wp = m_String.GetBuffer(m_String.GetLength()) ;

		char	*dp, *p ;
		for(dp=p=(char*)wp; p; dp=p+1){
			if( (p=strchr(dp,IN_SplitCode)) ) {
				p[0] = NULL ;
			}
			str = dp ;
			m_SplitData.Add(str) ;
		}
		m_String.ReleaseBuffer() ;
		return &m_SplitData ;
	}
//
//	<指定された文字列で分割する>
//
	CStringArray*	Split(char	*IN_SplitStr) {
		m_SplitData.SetSize(0) ;
		CString		str ;
		LPTSTR		wp = m_String.GetBuffer(m_String.GetLength()) ;

		char	*dp, *p ;
		for(dp=p=(char*)wp; p; dp=p+1){
			if( (p=strstr(dp,IN_SplitStr)) ) {
				memset(p,0x00,strlen(IN_SplitStr)) ;
				p += (strlen(IN_SplitStr)-1) ;
			}
			else{
				break;
			}
			str = dp ;
			m_SplitData.Add(str) ;
		}
		m_String.ReleaseBuffer() ;
		return &m_SplitData ;
	}

protected :
//
//	<文字列>
//
	CString		m_String ;
//
//	<分割した文字列リスト>
//
	CStringArray	m_SplitData ;
} ;
#endif