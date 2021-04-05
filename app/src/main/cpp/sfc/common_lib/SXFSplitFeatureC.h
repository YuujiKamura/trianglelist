//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFSplitFeatureC.h                     作成日：2000.04.17
//
//	１．クラス概要
//      Feature Instance Dataを分割する
// 
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
#ifndef _SXFSPLITFEATUREC_
#define _SXFSPLITFEATUREC_

#include <afx.h>

class SXFSplitFeatureC {
public :
	SXFSplitFeatureC(CString& IN_Str) {
		m_String = IN_Str ;
	}
	SXFSplitFeatureC(CString* IN_Str) {
		m_String = *IN_Str ;
	}
	SXFSplitFeatureC(char* IN_Str) {
		m_String = IN_Str ;
	}
	
    
    ~SXFSplitFeatureC() {}

    //split a instance data use  "'" and "'"    
    CStringArray*	Split(	) {
		m_SplitData.SetSize(0) ;
		CString		str ;
		int			n;

        // replace japanese char string
        // japanese    \'日本語文字\'
        // will replace by  'SXF_FQS_JAPANESE_CHAR_STRING_FQS_SXF'
        //
        CString replacestring="'SXF_FQS_JAPANESE_CHAR_STRING_FQS_SXF'";
        CString searchstring="SXF_FQS_JAPANESE_CHAR_STRING_FQS_SXF";
        
        CUIntArray placearray;
        CStringArray jstringarray;
        CString jstring;
        
        int result = -2; 

        while(1){
            result = m_String.Find("\\'",result+2);
            if(result == -1)
                break;
            placearray.Add(result);
        }

        INT_PTR markcount = placearray.GetSize();
        INT_PTR stringcount = 0;
        
        if(markcount !=0 ){
            
            if(markcount%2 !=0)
                return NULL;//error
            
            int place1=0;
            int place2=0;
            
            for(INT_PTR i= markcount-1;i>0;i=i-2){
                place1=placearray[i-1];
                place2=placearray[i];
                jstring=m_String.Mid(place1,place2-place1+2);
//	DEL(S)	既存バグ修正	K.Naono	03.01.22
/*
				n = m_String.Replace(jstring, replacestring);
                if( n == 0)
                    return NULL;//error

                jstring.Delete(0,2);;
                jstring.Delete(jstring.GetLength()-2,2);
*/
//	DEL(E)	既存バグ修正	K.Naono	03.01.22
                jstringarray.Add(jstring);

            }

            stringcount = jstringarray.GetSize();

//	ADD(S)	既存バグ修正	K.Naono	03.01.22
			for(i = 0; i < stringcount; i++) {
				n = m_String.Replace(jstringarray[i], replacestring) ;

				jstringarray[i].Delete(0, 2) ;
				jstringarray[i].Delete(jstringarray[i].GetLength() - 2, 2) ;
			}
//	ADD(E)	既存バグ修正	K.Naono	03.01.22
        }        

        //split
        LPTSTR		wp = m_String.GetBuffer(m_String.GetLength()) ;

		char	*dp, *p ;
        int count = 0;
		for(dp=p=(char*)wp; p; dp=p+1){
			if( (p=strchr(dp,0x27)) ) {// ` 0x27
				p[0] = NULL ;
			}
			str = dp ;
			
            count++;
            if(count%2 == 0){
                m_SplitData.Add(str) ;
            }
		}
		m_String.ReleaseBuffer() ;


        int replacecount=0;


        
        if(markcount !=0 ){
            for(INT_PTR i=m_SplitData.GetSize()-1;i>=0;i--){
                if( m_SplitData[i] == searchstring){
                    if(replacecount == stringcount)
                        return NULL;//error
                    m_SplitData[i] = jstringarray[replacecount];
                    replacecount++;
                }
            }
            
            if(replacecount != stringcount)
                return NULL;//error
        }        
        
        
        return &m_SplitData ;
	}

protected :
	CString		m_String ;

    CStringArray	m_SplitData ;

} ;

#endif
