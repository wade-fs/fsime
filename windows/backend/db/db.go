// Package db mirrors the BDatabase.kt query logic for the Windows backend.
// Uses modernc.org/sqlite (pure Go, CGO_ENABLED=0 compatible).
package db

import (
	"database/sql"
	"fmt"
	"strings"

	_ "modernc.org/sqlite"
)

const (
	fuzzyExact  = 0
	fuzzyPrefix = 1
	fuzzyFull   = 2
)

// B holds a single candidate row (mirrors com.wade.fsime.data.B).
type B struct {
	ID    int
	Ch    string
	Eng   string
	Freq  float64
}

// BDatabase wraps the b.db SQLite connection.
type BDatabase struct {
	db *sql.DB
}

// Open opens the shared b.db file (read-write so user_learning can be updated).
func Open(path string) (*BDatabase, error) {
	db, err := sql.Open("sqlite", path+"?_journal=WAL&_busy_timeout=5000")
	if err != nil {
		return nil, fmt.Errorf("open db: %w", err)
	}
	bdb := &BDatabase{db: db}
	if err := bdb.initUserTable(); err != nil {
		return nil, err
	}
	return bdb, nil
}

func (b *BDatabase) Close() error { return b.db.Close() }

// initUserTable creates the user_learning table if not exists.
func (b *BDatabase) initUserTable() error {
	_, err := b.db.Exec(`CREATE TABLE IF NOT EXISTS user_learning
		(context TEXT, ch TEXT, freq INTEGER,
		 PRIMARY KEY (context, ch))`)
	return err
}

// UpdateUsage records a selection (bigram + code association), matching BDatabase.kt.
func (b *BDatabase) UpdateUsage(prevChar, code, ch string) error {
	ctx := ""
	if len(prevChar) > 0 {
		runes := []rune(prevChar)
		ctx = string(runes[len(runes)-1:])
	}
	upsert := `INSERT INTO user_learning (context, ch, freq) VALUES (?, ?, 1)
		ON CONFLICT(context, ch) DO UPDATE SET freq = freq + 1`
	if _, err := b.db.Exec(upsert, ctx, ch); err != nil {
		return err
	}
	if code != "" {
		if _, err := b.db.Exec(upsert, "code:"+code, ch); err != nil {
			return err
		}
	}
	return nil
}

// query is the shared low-level query (mirrors BDatabase.kt#query).
func (b *BDatabase) query(k string, start, max int, table, field string, fuzzy int) ([]B, error) {
	useFreq := table == "ngram"
	userCtxKey := "code:" + k
	if useFreq {
		userCtxKey = k
	}
	baseFreqCol := "0"
	if useFreq {
		baseFreqCol = "t.freq"
	}

	var whereClause, pattern string
	switch fuzzy {
	case fuzzyExact:
		whereClause = fmt.Sprintf("t.%s = ?", field)
		pattern = k
	case fuzzyPrefix:
		whereClause = fmt.Sprintf("t.%s LIKE ?", field)
		if useFreq {
			pattern = k + "%"
		} else {
			pattern = k + "_%"
		}
	default:
		whereClause = fmt.Sprintf("t.%s LIKE ?", field)
		pattern = "%" + k + "%"
	}

	q := fmt.Sprintf(`
		SELECT t.*, (IFNULL(u.freq, 0) * 100000 + %s) AS total_freq
		FROM %s t
		LEFT JOIN user_learning u ON u.context = ? AND u.ch = t.ch
		WHERE %s
		ORDER BY total_freq DESC
		LIMIT ? OFFSET ?`, baseFreqCol, table, whereClause)

	rows, err := b.db.Query(q, userCtxKey, pattern, max, start)
	if err != nil {
		return nil, fmt.Errorf("query %s: %w", table, err)
	}
	defer rows.Close()

	cols, err := rows.Columns()
	if err != nil {
		return nil, err
	}

	var results []B
	seen := map[string]bool{}
	for rows.Next() && len(results) < max {
		dest := make([]interface{}, len(cols))
		for i := range dest {
			var v interface{}
			dest[i] = &v
		}
		if err := rows.Scan(dest...); err != nil {
			return nil, err
		}
		bRow := rowToB(cols, dest)
		if !seen[bRow.Ch] {
			seen[bRow.Ch] = true
			results = append(results, bRow)
		}
	}
	return results, rows.Err()
}

func rowToB(cols []string, vals []interface{}) B {
	var b B
	for i, col := range cols {
		v := *(vals[i].(*interface{}))
		switch strings.ToLower(col) {
		case "id":
			if n, ok := v.(int64); ok {
				b.ID = int(n)
			}
		case "ch":
			if s, ok := v.(string); ok {
				b.Ch = s
			}
		case "eng":
			if s, ok := v.(string); ok {
				b.Eng = s
			}
		case "freq", "total_freq":
			switch n := v.(type) {
			case int64:
				b.Freq = float64(n)
			case float64:
				b.Freq = n
			}
		}
	}
	return b
}

// GetWord mirrors BDatabase.kt#getWord.
// table should be one of: boshiamy, ji, cj, stroke, sym.
func (b *BDatabase) GetWord(k string, start, max int, table string) ([]string, error) {
	if k == "" {
		return nil, nil
	}
	key := strings.ToLower(k)
	result := []string{k} // first item is always the raw input

	validTables := map[string]bool{"boshiamy": true, "ji": true, "cj": true, "stroke": true, "sym": true}
	targetTable := "boshiamy"
	if validTables[table] {
		targetTable = table
	}

	var tables []string
	limitMax := max
	if targetTable == "boshiamy" {
		tables = []string{"boshiamy", "sym", "ji", "cj", "stroke"}
		limitMax *= 3
	} else {
		tables = []string{targetTable, "sym"}
	}

	var candidates []B

	// Phase 1: Exact match
	for _, t := range tables {
		res, err := b.query(key, start, limitMax, t, "eng", fuzzyExact)
		if err != nil {
			continue
		}
		for _, row := range res {
			runes := []rune(row.Ch)
			if len(runes) > 1 {
				for _, r := range runes {
					candidates = append(candidates, B{Ch: string(r), Eng: row.Eng})
					limitMax--
				}
			} else {
				candidates = append(candidates, row)
				limitMax--
			}
			if limitMax <= 0 {
				break
			}
		}
		if limitMax <= 0 {
			break
		}
	}

	// Phase 2: Prefix match
	if limitMax > 0 {
		adjStart := 0
		if start > len(candidates) {
			adjStart = start - len(candidates)
		}
		for _, t := range tables {
			res, err := b.query(key, adjStart, limitMax, t, "eng", fuzzyPrefix)
			if err != nil {
				continue
			}
			for _, row := range res {
				runes := []rune(row.Ch)
				if len(runes) > 1 {
					for _, r := range runes {
						candidates = append(candidates, B{Ch: string(r), Eng: row.Eng})
						limitMax--
					}
				} else {
					candidates = append(candidates, row)
					limitMax--
				}
				if limitMax <= 0 {
					break
				}
			}
			if limitMax <= 0 {
				break
			}
		}
	}

	seen := map[string]bool{k: true}
	for _, c := range candidates {
		if c.Ch != "" && !seen[c.Ch] {
			seen[c.Ch] = true
			result = append(result, c.Ch)
		}
	}
	return result, nil
}

// GetPhrase mirrors BDatabase.kt#getPhrase (ngram next-word prediction).
func (b *BDatabase) GetPhrase(contextStr string, start, max int) ([]string, error) {
	runes := []rune(contextStr)
	ctx := contextStr
	if len(runes) > 2 {
		ctx = string(runes[len(runes)-2:])
	}

	var predictions []B
	seen := map[string]bool{}

	addUnique := func(rows []B) {
		for _, r := range rows {
			if !seen[r.Ch] {
				seen[r.Ch] = true
				predictions = append(predictions, r)
			}
		}
	}

	// 1. 2-char context
	if len([]rune(ctx)) == 2 {
		rows, _ := b.query(ctx, 0, max, "ngram", "context", fuzzyExact)
		addUnique(rows)
	}

	// 2. 1-char context
	if len(predictions) < max && len(runes) > 0 {
		lastChar := string(runes[len(runes)-1:])
		rows, _ := b.query(lastChar, 0, max-len(predictions), "ngram", "context", fuzzyExact)
		addUnique(rows)
	}

	// 3. Unigram fallback
	if len(predictions) < max {
		rows, _ := b.query("", 0, max-len(predictions), "ngram", "context", fuzzyExact)
		addUnique(rows)
	}

	result := make([]string, 0, len(predictions))
	for _, p := range predictions {
		if p.Ch != "" {
			result = append(result, p.Ch)
		}
	}
	return result, nil
}
