package org.open.scad.lexer;

/***************************************************************
 Class: CAcceptAnchor
 **************************************************************/
class CAcceptAnchor {
    /***************************************************************
     Member Variables
     **************************************************************/
    CAccept m_accept;
    int m_anchor;

    /***************************************************************
     Function: CAcceptAnchor
     **************************************************************/
    CAcceptAnchor
    (
    ) {
        m_accept = null;
        m_anchor = CSpec.NONE;
    }
}
