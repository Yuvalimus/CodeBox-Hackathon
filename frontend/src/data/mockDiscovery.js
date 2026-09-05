const students = [
  ['Maya', 'Computer Science', 'Second', 'I like working through problems on a whiteboard.'],
  ['Jordan', 'Mathematics', 'Third', 'Practice problems and a little coffee.'],
  ['Alex', '', '', ''],
  ['Sam', 'Computer Engineering', 'First', 'Happy to compare approaches and learn something new.'],
  ['Riley', 'Biology', 'Fourth', 'Let’s turn a big assignment into smaller steps.'],
  ['Casey', 'Architecture', 'Third', 'Looking for some focused study company.'],
  ['Taylor', 'Business Administration', 'Second', 'A quiet session with breaks to compare notes.'],
  ['Morgan', 'Physics', 'Fifth+', 'Explaining things out loud helps me learn.'],
  ['Jamie', '', 'First', ''],
  ['Avery', 'Electrical Engineering', 'Third', 'Bring your questions. I’ll bring mine.'],
  ['Drew', 'Statistics', 'Second', 'Always up for another practice problem.'],
  ['Quinn', 'English', 'Fourth', 'A fresh perspective makes a difference.'],
];

// Replace this synchronous mock interface when the discovery API is available.
// Classes are deliberately seeded from the test session so any class can be tried.
export function getMockCandidates(session) {
  return students.map(([name, major, year, bio], index) => ({
    id: `test-student-${index}`, name, major, year, bio,
    avatar: ['sage', 'blue', 'peach', 'lavender'][index % 4],
    online: index !== 11,
    classes: index % 3 === 0 ? [...session.classes] : [session.classes[index % session.classes.length]],
    location: index % 3 === 0 ? session.location : index % 3 === 1 ? 'Kennedy Library' : '',
  })).filter((student) => student.online).sort((a, b) => b.classes.length - a.classes.length);
}
