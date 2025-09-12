from __future__ import annotations
from dataclasses import dataclass, field, asdict
from typing import List, Dict, Optional, Any
import json
from datetime import datetime, timezone

@dataclass
class Span:
    r0: int
    c0: int
    r1: int
    c1: int
    label: str = ""

@dataclass
class Head:
    rows: List[List[str]] = field(default_factory=list)
    spans: List[Span] = field(default_factory=list)

@dataclass
class Cell:
    r: int
    c: int
    v: Any

@dataclass
class Grid:
    nRows: int
    nCols: int
    body: List[List[Cell]] = field(default_factory=list)
    columnsEditable: Dict[str, str] = field(default_factory=dict)

@dataclass
class Table:
    head: Head
    grid: Grid

@dataclass
class Anlage:
    name: str
    melder: Table
    hardware: Optional[Table] = None

@dataclass
class EditedBy:
    name: str
    ts: str

@dataclass
class Meta:
    pType: str
    wType: str
    VNnr: str
    Kunde: str

@dataclass
class Protokoll:
    melderTypes: List[str]
    anlagen: List[Anlage]
    editedBy: EditedBy

@dataclass
class Envelope:
    Meta: Meta
    Protokoll: Protokoll

    def to_json_bytes(self) -> bytes:
        def default(o):
            if hasattr(o, "__dict__"):
                return asdict(o)
            return str(o)
        return json.dumps(asdict(self), ensure_ascii=False, separators=(",",":")).encode("utf-8")

def make_head(header_labels: List[str], spans: Optional[List[dict]] = None) -> Head:
    span_objs = []
    for s in spans or []:
        span_objs.append(Span(**s))
    return Head(rows=[header_labels], spans=span_objs)

def make_grid(rows: List[List[Any]], columns_editable: Dict[int, str]) -> Grid:
    body: List[List[Cell]] = []
    for r_idx, row in enumerate(rows):
        body.append([Cell(r=r_idx, c=c_idx, v=val) for c_idx, val in enumerate(row)])
    cols = max((len(r) for r in rows), default=0)
    col_edit = {str(k): v for k, v in columns_editable.items()}
    return Grid(nRows=len(rows), nCols=cols, body=body, columnsEditable=col_edit)

def now_utc_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00","Z")
