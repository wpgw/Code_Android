function Foggy(svInput) 
    {
      // Any changes must be duplicated in the server-side version of this function.
      var svOutput = "";
      var ivRnd;
      var i;
      var ivLength = svInput.length;
      if (ivLength == 0 || ivLength > 158) 
      {
        svInput = svInput.replace(/"/g,"&qt;");
        return svInput;
      }   
      for (i = 0; i < ivLength; i++) 
      {
        ivRnd = Math.floor(Math.random() * 3);
        if (svInput.charCodeAt(i) == 32 || svInput.charCodeAt(i) == 34 || svInput.charCodeAt(i) == 62) 
        {
          ivRnd = 1;
        }
        if (svInput.charCodeAt(i) == 33 || svInput.charCodeAt(i) == 58 || svInput.charCodeAt(i) == 59 || svInput.charCodeAt(i) + ivRnd > 255) 
        {
          ivRnd = 0;
        }
        svOutput += String.fromCharCode(ivRnd+97);
        svOutput += String.fromCharCode(svInput.charCodeAt(i)+ivRnd);
      }
      for (i = 0; i < Math.floor(Math.random() * 8) + 8; i++) 
      {
        ivRnd = Math.floor(Math.random() * 26);
        svOutput += String.fromCharCode(ivRnd+97);
      }
      svOutput += String.fromCharCode(svInput.length + 96);
      return svOutput;
    }