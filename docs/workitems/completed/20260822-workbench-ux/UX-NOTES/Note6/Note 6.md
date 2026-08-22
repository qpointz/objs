# Query view improvement 


![[Pasted image 20260821224545.png]]
**(1)** Remove outer box and the **“Query” title inside it** (duplicates view title — keep **G-UX-vchrome** “Query”)  
**(2)** Execution stats → **left** on chrome row — **same string** as today before **New graph**  
**(3)** → **(4)** **Exec** on chrome row right (before Options cog)  
**(5)** / **(7)** **Open in Composer** — prefer `result.contents`; **disabled** if no graph result; **refuse** over-cap/huge; Composer **New graph** with all V/E  
**(6)** Remove right Options pane → Options **popover anchored to cog** (timeout only); cog after Exec  
**(8)** Remove other copies of the **(2)** stats string  
**(9)** Virtualize large Structured rendering  

Let's change structured view. when it renders Vertex and Edges use grid similiar to objects view  

**Vertices** columns: id · type · name (name = Object viewer logic)  
**Edges** columns: id · type · source name · role · target name  
Split into **two tables** (vertices / edges).  

id / row select opens object viewer as a **right pane inside** the Graph or Structured **tab** **(11)** (reuse Note 5 as-is; hide when nothing selected; no multi-select). Same on Graph for node and edge.

**Mode:** subgraph **or** projections — if both, **table-alike wins**.  

**Structured grid chrome (both modes):** one shared grid style (Objects table reference). Virtualize when **> 200** rows.

