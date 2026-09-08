// Package engine mirrors InputProcessor.kt for the Windows backend.
// It manages composition state and candidate generation.
package engine

import (
	"strings"
	"unicode/utf8"

	"github.com/wade/fsime-server/db"
)

// InputProcessor mirrors com.wade.fsime.engine.InputProcessor.
type InputProcessor struct {
	bdb       *db.BDatabase
	state     State
	prevChar  string // last committed character (for ngram learning)
}

// State mirrors KeyboardState (immutable snapshot).
type State struct {
	Composing  string
	Candidates []string
	Mode       string // active keyboard: boshiamy, ji, cj, stroke, digit
}

func New(bdb *db.BDatabase) *InputProcessor {
	return &InputProcessor{
		bdb:   bdb,
		state: State{Mode: "boshiamy"},
	}
}

func (p *InputProcessor) GetState() State { return p.state }

// SetMode switches the active input mode.
func (p *InputProcessor) SetMode(mode string) {
	p.state = State{Mode: mode}
}

// AppendStroke appends a key stroke to the composition buffer.
func (p *InputProcessor) AppendStroke(stroke string) {
	newComposing := p.state.Composing + stroke
	candidates, _ := p.computeCandidates(newComposing)
	p.state = State{
		Composing:  newComposing,
		Candidates: candidates,
		Mode:       p.state.Mode,
	}
}

// Backspace removes the last rune from the composition buffer.
func (p *InputProcessor) Backspace() {
	c := p.state.Composing
	if c == "" {
		return
	}
	// Remove last UTF-8 rune
	_, size := utf8.DecodeLastRuneInString(c)
	newComposing := c[:len(c)-size]
	candidates, _ := p.computeCandidates(newComposing)
	p.state = State{
		Composing:  newComposing,
		Candidates: candidates,
		Mode:       p.state.Mode,
	}
}

// Clear resets the composition buffer without committing.
func (p *InputProcessor) Clear() {
	p.state = State{Mode: p.state.Mode}
}

// PickCandidate selects the candidate at index, clears composition, and returns
// the character to commit.  Returns "" if index is out of range.
func (p *InputProcessor) PickCandidate(index int) string {
	if index < 0 || index >= len(p.state.Candidates) {
		return ""
	}
	ch := p.state.Candidates[index]
	// Record learning
	code := p.state.Composing
	_ = p.bdb.UpdateUsage(p.prevChar, code, ch)
	p.prevChar = ch
	p.Clear()
	return ch
}

// GetPhrase returns next-word predictions based on the last committed context.
func (p *InputProcessor) GetPhrase(contextStr string) ([]string, error) {
	return p.bdb.GetPhrase(contextStr, 0, 30)
}

// computeCandidates mirrors InputProcessor.kt#computeCandidateList.
func (p *InputProcessor) computeCandidates(composing string) ([]string, error) {
	if composing == "" {
		return nil, nil
	}

	list, err := p.bdb.GetWord(composing, 0, 30, p.state.Mode)
	if err != nil {
		return nil, err
	}

	// digit mode: math expression ending with '!'
	if p.state.Mode == "digit" && strings.HasSuffix(composing, "!") {
		if result := evalMath(strings.TrimSuffix(composing, "!")); result != "" {
			list = append([]string{composing, result}, list...)
		}
	}

	return list, nil
}

// evalMath is a minimal arithmetic evaluator for the digit keyboard.
// Replace with a full parser if needed (mirrors MathParser).
func evalMath(expr string) string {
	// Placeholder: integrate a proper math parser here.
	// Return empty string if unable to evaluate.
	return ""
}
