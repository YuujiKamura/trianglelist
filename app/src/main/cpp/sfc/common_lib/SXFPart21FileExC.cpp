//---------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileC.cpp					作成日：2001.09.17
//
//	１．クラス概要
//		ファイルの入力を管理する
//
//	２．履歴
//		履歴番号	日付け		 担当者			概要
//		作成		2001.09.17	K.Naono(FQS)	ヘッダ解析処理の改善対応
//
//---------------------------------------------------------
#include "stdafx.h"
#include "SXFPart21FileExC.h"
#include "SXFHeaderLex.h"
#include "ComSplitStringC.h"
#include "SXFutyConvKanjiC.h"

//////////////////////////////////////////////////////////////////////
// 構築/消滅
//////////////////////////////////////////////////////////////////////

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileC.cpp					作成日：2001.09.17
//
//	１．機能概要
//		コンストラクタ
//
//	２．インターフェイス
//		SXFPart21FileExC::SXFPart21FileExC(
//						LPCTSTR	IN_lpszFileName,
//						UINT	IN_nOpenFlags)
//
//		引数: 型				引数名			    内容
//				LPCTSTR		IN_lpszFileName		ファイル名
//				UINT		IN_nOpenFlags		ファイルオープンモード
//
//		復帰値	なし
//
//	３．履歴
//		履歴番号	日付け		 担当者			概要
//		作成		2001.09.17	K.Naono(FQS)
//---------------------------------------------------------
SXFPart21FileExC::SXFPart21FileExC(
						LPCTSTR	IN_lpszFileName,
						UINT	IN_nOpenFlags)
				:SXFPart21FileC(IN_lpszFileName, IN_nOpenFlags)
{
}

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileC.cpp					作成日：2001.09.17
//
//	１．機能概要
//		デストラクタ
//
//	２．インターフェイス
//		SXFPart21FileExC::~SXFPart21FileExC()
//
//		引数:	なし
//
//		復帰値	なし
//
//	３．履歴
//		履歴番号	日付け		 担当者			概要
//		作成		2001.09.17	K.Naono(FQS)
//---------------------------------------------------------
SXFPart21FileExC::~SXFPart21FileExC()
{
}

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileC.cpp					作成日：2001.09.17
//
//	１．機能概要
//		Head Section部分を読み込む
//
//	２．インターフェイス
//		int SXFPart21FileExC::ReadHeadSection(
//							HEADSECTION*	IO_Head)
//
//		引数: 型				引数名			    内容
//			HEADSECTION*	IO_Head				HeadSectionの
//												構造体のアドレス
//		復帰値
//			正常時： 1 : OK
//			異常時：
//					-1 : file end
//					-2 : found syntax error 
//					-3 : can not to find a headsection
//
//	３．履歴
//		履歴番号	日付け		 担当者			概要
//		作成		2001.09.17	K.Naono(FQS)
//---------------------------------------------------------
int SXFPart21FileExC::ReadHeadSection(HEADSECTION* IO_Head)
{
	ASSERT(IO_Head) ;
	if( IO_Head == NULL ) {
		return -1 ;
	}

	//	HEADER ～ ENDSECまでを読み込む
	CString	buffer ;
	CString	record ;

	while( ReadString(record) ) {
		record.TrimLeft() ;
		record.TrimRight() ;
		buffer += record ;
		if( record.Find(ID_ENDSEC) != -1 )
			break ;
	}
	if( buffer.GetLength() == 0 ) {
		return -3 ;
	}

	//	字句の解析を始める
    BOOL			bExistHeader = FALSE ;
	CStringArray	array ;
	int token = 0, ret = 0 ;
	CSXFHeaderLex*	pLex = new CSXFHeaderLex(buffer) ;
	while( (token = pLex->Lex() ) != TT_EOF) {

		switch(token) {
		case TT_FILE_DESCRIPTION :
			//	属性値の取得
			getAttribute(pLex, array) ;

			//	属性値の解析
			if( (ret = analyzeDescription(array, IO_Head)) == 1 ) {
				break ;
			}
			delete pLex ;
			if( ret == 0 ) {
				return ret ;
			} else {
				return !bExistHeader ? -3 : ret ;
			}
			break ;

		case TT_FILE_NAME :
			//	属性値の取得
			getAttribute(pLex, array) ;

			//	属性値の解析
			if( (ret = analyzeName(array, IO_Head)) < 0 ) {
				delete pLex ;
				return !bExistHeader ? -3 : ret ;
			}
			break ;

		case TT_FILE_SCHEMA :
			break ;

		case TT_HEADER :
			bExistHeader = TRUE ;
			break ;

		case TT_ENDSEC :
			bExistHeader = FALSE ;
			break ;
		}
	}

	delete pLex ;

	return 1 ;
}

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileC.cpp					作成日：2001.09.17
//
//	１．機能概要
//		Part21 FileのFILE_DESCRIPTION部分を解析する
//
//	２．インターフェイス
//		int SXFPart21FileExC::analyzeDescription(
//							CStringArray&	array,
//							HEADSECTION*	IO_Head)
//
//		引数: 型				引数名			    内容
//			CStringArray&		array			属性値
//			HEADSECTION*		IO_Head			HeadSectionの
//												構造体のアドレス
//		復帰値	
//			正常時： 1 : OK
//			異常時：
//					-1 : file end
//					-2 : found syntax error 
//					-3 : can not to find a headsection
//
//	３．履歴
//		履歴番号	日付け		 担当者			概要
//		作成		2001.09.17	K.Naono(FQS)
//---------------------------------------------------------
int SXFPart21FileExC::analyzeDescription(
							CStringArray&	array,
							HEADSECTION*	IO_Head)
{
	if( array.GetSize() != 2 ) {
		return -2 ;
	}

	CStringArray*	pElem ;
	ComSplitStringC split(array[0]) ;
	pElem = split.Split(' ') ;
	if( pElem->GetSize() < 3 ) {
		return -2 ;
	}
	//	SCADEC以外のファイル
	if( (*pElem)[0] != ID_SCADEC ) {
		return 0 ;
	}

	//	SCADEC flg をON
	IO_Head->scadec_flg = 1 ;
	for(int i = 0; i < pElem->GetSize(); i++) {
		//	レベル
		if( !(*pElem)[i].CompareNoCase(ID_LEVEL1) ) {
			IO_Head->Level = 1 ;

		} else if( !(*pElem)[i].CompareNoCase(ID_LEVEL2) ) {
			IO_Head->Level = 2 ;

		//	モード
		} else if( !(*pElem)[i].CompareNoCase(ID_AMBI_MODE) ) {
			IO_Head->Mode = 0 ;

		} else if( !(*pElem)[i].CompareNoCase(ID_FEATURE_MODE) ) {
			IO_Head->Mode = 1 ;

		} else if( !(*pElem)[i].CompareNoCase(ID_AP202_MODE) ) {
			IO_Head->Mode = 2 ;
		}
	}

	return 1 ;
}

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileC.cpp					作成日：2001.09.17
//
//	１．機能概要
//		Part21 FileのFILE_NAME部分を解析する
//
//	２．インターフェイス
//		int SXFPart21FileExC::analyzeName(
//							CStringArray&	array,
//							HEADSECTION*	IO_Head)
//
//		引数: 型				引数名			    内容
//			CStringArray&		array			属性値
//			HEADSECTION*		IO_Head			HeadSectionの
//												構造体のアドレス
//		復帰値	
//			正常時： 1 : OK
//			異常時：
//					-1 : file end
//					-2 : found syntax error 
//					-3 : can not to find a headsection
//
//	３．履歴
//		履歴番号	日付け		 担当者			概要
//		作成		2001.09.17	K.Naono(FQS)
//---------------------------------------------------------
int SXFPart21FileExC::analyzeName(
							CStringArray&	array,
							HEADSECTION*	IO_Head)
{
	if( array.GetSize() != 7 ) {
		return -2 ;
	}

	TCHAR	buffer[257] ;
	//	name属性値の取得
	SXFutyConvKanjiC::ConvKanji("ToJIS",
//					(char*)(LPCTSTR)array[0], buffer) ;	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
					(char*)(LPCTSTR)array[0], buffer, _countof(buffer)) ;
	IO_Head->FileName = buffer ;

	//	author属性値の取得
	SXFutyConvKanjiC::ConvKanji("ToJIS",
//					(char*)(LPCTSTR)array[2], buffer) ;	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
					(char*)(LPCTSTR)array[2], buffer, _countof(buffer)) ;
	IO_Head->Author = buffer ;

	//	organization属性値の取得
	SXFutyConvKanjiC::ConvKanji("ToJIS",
//					(char*)(LPCTSTR)array[3], buffer) ;	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
					(char*)(LPCTSTR)array[3], buffer, _countof(buffer)) ;
	IO_Head->Organization = buffer ;

	//	preprocessor_version属性値の取得
	SXFutyConvKanjiC::ConvKanji("ToJIS",
//					(char*)(LPCTSTR)array[4], buffer) ;	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
					(char*)(LPCTSTR)array[4], buffer, _countof(buffer)) ;
	IO_Head->Version = buffer ;

	//	originating_system属性値の取得
	SXFutyConvKanjiC::ConvKanji("ToJIS",
//					(char*)(LPCTSTR)array[5], buffer) ;	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
					(char*)(LPCTSTR)array[5], buffer, _countof(buffer)) ;
	IO_Head->OriginatingSystem = buffer ;

	return 1 ;
}

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileC.cpp					作成日：2001.09.17
//
//	１．機能概要
//      Part21 Fileのエンティティの属性値を取得する
//
//	２．インターフェイス
//		void SXFPart21FileExC::getAttribute(
//							CSXFHeaderLex*	pLex,
//							CStringArray&	array)
//
//		引数: 型				引数名			    内容
//			CSXFHeaderLex*		pLex			Lexオブジェクト
//			CStringArray&		array			属性値格納用配列
//
//		復帰値
//			なし
//
//	３．履歴
//		履歴番号	日付け		 担当者			概要
//		作成		2001.09.17	K.Naono(FQS)
//---------------------------------------------------------
void SXFPart21FileExC::getAttribute(
					CSXFHeaderLex*	pLex,
					CStringArray&	array)
{
	if( array.GetSize() > 0 ) {
		array.RemoveAll() ;
	}

//	CString	key ;
	int token ;
	while( (token = pLex->Lex()) != ';' ) {
//	ADD(S)	バグ修正	K.Naono	03.05.29
		if( token == TT_EOF ) break ;
//	ADD(E)	バグ修正	K.Naono	03.05.29

		// 読み飛ばし
		if( token == ' ' || token == '\t' )	continue ;

		if( token == TT_LINE_COMMENT ) {
			while((token = pLex->Lex()) != TT_EOL) ;
			continue ;
		}
		if( token == TT_BLOCK_COMMENT_START ) {
//			while((token = pLex->Lex()) != TT_BLOCK_COMMENT_END) ;
			while( (token = pLex->Lex()) != TT_EOF ) {
				if( token == TT_BLOCK_COMMENT_END )
					break ;
			}
			continue ;
		}

		switch(token) {
		// キーワード
		case TT_IDENTIFIER :
		case TT_STRING :
			array.Add(pLex->GetTokenImage()) ;
			break ;

		default :
			break ;
		}
	}
}
